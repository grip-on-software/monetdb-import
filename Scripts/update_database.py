"""
Script to perform database schema upgrades.

Copyright 2017-2020 ICTU
Copyright 2017-2022 Leiden University
Copyright 2017-2024 Leon Helwerda

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

from argparse import ArgumentParser, Namespace
from configparser import RawConfigParser
from glob import glob
import logging
from pathlib import Path
from typing import overload, Any, Callable, Dict, Mapping, Optional, \
    Sequence, Union
from pymonetdb.sql.connections import Connection
from pymonetdb.sql.cursors import Cursor
from pymonetdb.sql.monetize import convert
import yaml

def parse_args() -> Namespace:
    """
    Parse command line arguments.
    """

    config = RawConfigParser()
    config.read("settings.cfg")

    description = 'Update the database schema'
    log_levels = ['DEBUG', 'INFO', 'WARNING', 'ERROR', 'CRITICAL']
    parser = ArgumentParser(description=description, conflict_handler='resolve')
    parser.add_argument('-h', '--hostname', nargs='?',
                        default=config.get('monetdb', 'hostname'),
                        help='host name of the monetdb database to connect to')
    parser.add_argument('-u', '--username',
                        default=config.get('monetdb', 'username'),
                        help='username that alters the tables')
    parser.add_argument('-P', '--password',
                        default=config.get('monetdb', 'password'),
                        help='password of the user altering the tables')
    parser.add_argument('-d', '--database',
                        default=config.get('monetdb', 'database'),
                        help='database name to alter')
    parser.add_argument('-t', '--dry-run', action='store_true', dest='dry_run',
                        help='do not perform anything, show what would happen')
    parser.add_argument('-p', '--path', default='update',
                        help='path to the update files')
    parser.add_argument('-l', '--log', choices=log_levels, default='INFO',
                        help='log level (info by default)')

    args = parser.parse_args()
    if args.hostname is None:
        # An argument -h may mean help as well
        parser.print_help()
        parser.exit()

    logging.basicConfig(format='%(asctime)s:%(levelname)s:%(message)s',
                        level=getattr(logging, args.log.upper(), None))

    return args

@overload
def collect(cursor: Cursor, table: str, attributes: None = None,
            conditions: Optional[Mapping[str, Any]] = None,
            prefix: str = '') -> Dict[str, int]:
    ...
@overload
def collect(cursor: Cursor, table: str, attributes: Sequence[str],
            conditions: Optional[Mapping[str, Any]] = None,
            prefix: str = '') -> Dict[str, Dict[str, Any]]:
    ...
def collect(cursor: Cursor, table: str,
            attributes: Optional[Sequence[str]] = None,
            conditions: Optional[Mapping[str, Any]] = None,
            prefix: str = '') -> \
        Union[Dict[str, int], Dict[str, Dict[str, Any]]]:
    """
    Collect schema information from system tables.

    The table name indicates which type of information to collect. Additional
    fields from this table can be retrieved using a sequence of `attributes`
    and `conditions` should be specified as a mapping of columns and expected
    values.

    By default, this function collects the id and name of the schema objects.
    The `prefix` can be used in case the id and name fields have longer names.
    No escaping is done on any of the parameters.

    The returned dictionary has the names as keys. The values are either the ids
    or dictionaries with id and the other attributes, if any were given.
    """

    fields = ', '.join(f'"{field}"' for field in
                       (f'{prefix}id', f'{prefix}name') +
                        tuple(attributes if attributes is not None else ()))
    if conditions:
        where = ' AND '.join(f'{column} = {convert(value)}'
                             for column, value in conditions.items())
    else:
        where = '1=1'
    query = f"SELECT {fields} FROM sys.{table} WHERE {where}"
    cursor.execute(query)
    rows = cursor.fetchall()
    if not attributes:
        return {str(row[1]): int(row[0]) for row in rows}

    return {
        str(row[1]): dict(zip(attributes, row[2:]), id=row[0]) for row in rows
    }

def get_tables(cursor: Cursor, schemas: Dict[str, int]) -> \
        Dict[str, Dict[str, int]]:
    """
    Retrieve table information for all the schemas given in `schemas`.
    """

    return {
        name: collect(cursor, 'tables', conditions={'schema_id': id})
        for name, id in schemas.items()
    }

def _check_column(column: Dict[str, Any],
                  columns: Dict[str, Dict[str, Any]]) -> bool:
    name = str(column['name'])
    if column['action'] == 'add':
        return name not in columns
    if column['action'] == 'drop':
        return name in columns
    if name not in columns:
        raise KeyError(name)

    null = columns[name]['null']
    default = columns[name]['default']
    return column.get('null', null) != null or \
        column.get('default', default) != default

def _check_key(key: Dict[str, Any], keys: Dict[str, Dict[str, Any]],
               idxs: Dict[str, Dict[str, Any]], cursor: Cursor,
               types: Dict[str, Dict[str, int]]) -> bool:
    keys_indexes = {**keys, **idxs}
    key_name = str(key['name'])
    if key['action'] == 'create':
        return key_name not in keys_indexes
    if key['action'] == 'drop':
        return key_name in keys_indexes
    if key_name not in keys_indexes:
        raise KeyError(key_name)

    orig = keys_indexes[key_name]
    objects = collect(cursor, 'objects', ('nr',), {'id': orig['id']})
    objects_sort: Callable[..., int] = lambda part, objs=objects: int(objs[part]['nr'])
    parts = sorted(objects.keys(), key=objects_sort)
    change = bool(key.get('objects', parts) != parts)
    key_type = str(key.get('type'))
    if key_name in keys:
        # Should be(come) a key
        change = change or key_type in types['indexes'] or \
            orig['type'] != types['keys'].get(key_type, orig['type'])
    else:
        # Should be(come) an index
        change = change or key_type in types['keys'] or \
            orig['type'] != types['indexes'].get(key_type, orig['type'])

    return change

def check_update(update: Dict[str, Any], cursor: Cursor,
                 schema_tables: Dict[str, Dict[str, int]],
                 types: Dict[str, Dict[str, int]]) -> bool:
    """
    Check if a given update schema should have its associated update file
    executed in order to upgrade the database that we compare against.

    An outdated database may fail the update schema (and thus need an upgrade)
    if it doesn't have a table that the schema creates, it has a table that the
    schema drops, and likewise for columns/keys/indexes in that table except
    that they may also indicate a schema failure if the database has different
    properties (default value and nullable for columns, and key type and fields
    that are referenced for keys/indexes).

    If tables/columns/keys are unexpectedly missing, the check will not accept
    the upgrade as a precaution.

    Returns a boolean if the upgrade should take place.
    """

    table = str(update['table'])
    table_id = schema_tables.get(update['schema'], {}).get(table)
    action = str(update.get('action', 'alter'))
    if action == 'create':
        return table_id is None
    if action == 'drop':
        return table_id is not None

    if table_id is None:
        logging.error('Expected %s to exist', table)
        return False

    columns = collect(cursor, 'columns', ('default', 'null'),
                      {'table_id': table_id})
    keys = collect(cursor, 'keys', ('type',), {'table_id': table_id})
    idxs = collect(cursor, 'idxs', ('type',), {'table_id': table_id})
    change = False
    for column in update.get('columns', {}):
        try:
            change = change or _check_column(column, columns)
        except KeyError as error:
            logging.error('Expected column %s in %s', error, table)
            return False

    for key in update.get('keys', {}):
        try:
            change = change or _check_key(key, keys, idxs, cursor, types)
        except KeyError as error:
            logging.error('Expected key %s for %s', error, table)
            return False

    return change

def augment_error(yaml_error: yaml.MarkedYAMLError, filename: str,
                  line: int) -> None:
    """
    Correct marks of a YAML error to point to the loaded file and context line.
    """

    if yaml_error.problem_mark is not None:
        yaml_error.problem_mark.name = filename
        yaml_error.problem_mark.line += line
    if yaml_error.context_mark is not None:
        yaml_error.context_mark.name = filename
        yaml_error.context_mark.line += line

def handle_schema(update_filename: str, args: Namespace, cursor: Cursor,
                  schema_tables: Dict[str, Dict[str, int]],
                  types: Dict[str, Dict[str, int]]) -> bool:
    """
    Load an update file, check if its schema indicates that the update should
    be applied to the database, and execute the relevant commands if so.

    Returns whether the update was applied.
    """

    logging.info('Opening %s', update_filename)
    yaml_start = None
    schema = ''
    do_update = False
    command = ""
    with Path(update_filename).open('r', encoding='utf-8') as update_file:
        for number, line in enumerate(update_file):
            if not do_update and line.rstrip() == '-- %%':
                if yaml_start is None:
                    yaml_start = number

                if schema:
                    try:
                        update = yaml.safe_load(schema)
                    except yaml.error.MarkedYAMLError as yaml_error:
                        augment_error(yaml_error, update_filename, yaml_start)
                        logging.exception('Could not load schema')
                    else:
                        logging.info('Loading schema: %r', update)
                        do_update = check_update(update, cursor,
                                                 schema_tables, types)
                        if do_update:
                            logging.warning('Update file %s is needed.',
                                            update_filename)

                    schema = ''
                    yaml_start = None
            elif yaml_start is not None and line.startswith('-- '):
                schema += line[len('-- '):]
            elif do_update and line.strip() != '':
                command += line
                if line.rstrip().endswith(';'):
                    if args.dry_run:
                        logging.warning('[Dry run] Would execute %s',
                                        command.strip())
                    else:
                        cursor.execute(command)
                    command = ""

    return do_update

def main() -> None:
    """
    Main entry point.
    """

    args = parse_args()
    connection = Connection(args.database, hostname=args.hostname,
                            username=args.username, password=args.password,
                            autocommit=True)
    cursor = connection.cursor()
    schemas = collect(cursor, 'schemas', conditions={'system': 'FALSE'})
    schema_tables = get_tables(cursor, schemas)
    types = {
        'keys': collect(cursor, 'key_types', prefix='key_type_'),
        'indexes': collect(cursor, 'index_types', prefix='index_type_')
    }
    for update_filename in sorted(glob(f'{args.path}/*-[0-9]*.sql')):
        if handle_schema(update_filename, args, cursor, schema_tables, types):
            # Reload schema tables
            schema_tables = get_tables(cursor, schemas)

if __name__ == "__main__":
    main()
