"""
Script to validate a documentation page against the database schema.
"""

from argparse import ArgumentParser
from configparser import RawConfigParser
import json
import logging
import os.path
import re
import sys
from urllib.parse import urlparse
import git
from requests import Session
from requests.auth import HTTPBasicAuth

def parse_args(config):
    """
    Parse command line arguments.
    """

    description = 'Delete all data and recreate the database'
    log_levels = ['DEBUG', 'INFO', 'WARNING', 'ERROR', 'CRITICAL']
    verify = config.get('schema', 'verify')
    if verify in ('false', 'no', 'off', '-', '0', '', None):
        verify = False
    elif not os.path.exists(verify):
        verify = True

    parser = ArgumentParser(description=description)
    parser.add_argument('--path', default=config.get('schema', 'path'),
                        help='Path or URL to retrieve schema from')
    parser.add_argument('--url', default=config.get('schema', 'url'),
                        help='URL to retrieve documentation from')
    parser.add_argument('--verify', nargs='?', const=True, default=verify,
                        help='Enable SSL certificate verification')
    parser.add_argument('--no-verify', action='store_false', dest='verify',
                        help='Disable SSL certificate verification')
    parser.add_argument('--username', help='Username for documentation URL',
                        default=config.get('schema', 'username'))
    parser.add_argument('--password', help='Password for documentation URL',
                        default=config.get('schema', 'password'))
    parser.add_argument('--branch', nargs='?', default=None, const='master',
                        help='Branch to retrieve unmerged documentation for')
    parser.add_argument('-l', '--log', choices=log_levels, default='INFO',
                        help='log level (info by default)')
    parser.add_argument('--export', action='store_true', default=False,
                        help='Write JSON files of extracted schemas')

    args = parser.parse_args()
    logging.basicConfig(format='%(asctime)s:%(levelname)s:%(message)s',
                        level=getattr(logging, args.log.upper(), None))

    return args

def handle_match(line, current_tokens, data, parent_data, context):
    """
    Perform actions based on a match of a token on the given line.
    """

    name = context['name']
    token = context['token']
    groups = context['groups']

    if name == '__end':
        # May not work embeddedly
        return token['old'], None


    if len(groups) > 1:
        parent_data[name] = groups
    elif not groups:
        parent_data[name] = True
    elif 'within' in token or 'line' in token:
        if name not in parent_data:
            parent_data[name] = {}

        parent_data[name][groups[0]] = {}
    else:
        parent_data[name] = groups[0]

    if 'line' in token:
        test_line(line, token['line'], data, parent_data[name][groups[0]])
    if 'within' in token:
        old_current_tokens = current_tokens.copy()
        current_tokens = token['within']
        if 'end' in token:
            current_tokens['__end'] = {
                'pattern': token['end'],
                'old': old_current_tokens
            }

        return current_tokens, parent_data[name][groups[0]]

    return False

def test_line(line, current_tokens, data, parent_data=None):
    """
    Test whether the current token scope match the given line.
    """

    if parent_data is None:
        parent_data = data

    for name, token in current_tokens.items():
        match = token['pattern'].match(line)
        if match:
            groups = match.groups()
            result = handle_match(line, current_tokens, data, parent_data, {
                "name": name,
                "token": token,
                "groups": groups
            })
            if result:
                return result

    return current_tokens, parent_data

def parse(tokens, line_iterator, data=None):
    """
    Parse a file, iterable by lines, using line-based token patterns.
    """

    current_tokens = tokens

    if data is None:
        # No existing data
        data = {}

    parent_data = None

    for line in line_iterator:
        current_tokens, parent_data = \
            test_line(line, current_tokens, data, parent_data)

    return data

def parse_schema(session, path):
    """
    Parse an SQL table schema.
    """

    ident = r"[a-z_]+"
    ident_list = r'\("(' + ident + r')"(?:,\s*"(' + ident + r')")*\)'
    field = r'^\s*"' + ident + r'"'
    field_type = field + r'\s+[A-Z]+(?:\([0-9,]+\))?'
    tokens = {
        'table': {
            'pattern': re.compile(r'^CREATE TABLE "[a-z_]+"."([a-z_]+)" \('),
            'within': {
                'field': {
                    'pattern': re.compile(r'^\s*"([a-z_]+)"'),
                    'line': {
                        'type': {
                            'pattern': re.compile(field + r'\s+([A-Z]+(?:\([0-9,]+\))?)')
                        },
                        'null': {
                            'pattern': re.compile(field_type + r'\s*NULL')
                        },
                        'primary_key': {
                            'pattern': re.compile(field_type + r'.* AUTO_INCREMENT')
                        }
                    }
                },
                'primary_key_combined': {
                    'pattern': re.compile(r'^\s*CONSTRAINT "[a-z_]+" PRIMARY KEY\s*' + ident_list)
                }
            },
            'end': re.compile(r"^\);")
        }
    }

    try:
        url = urlparse(path)
        if url.scheme == '' or url.netloc == '':
            raise ValueError('URL must be absolute')

        request = session.get(path)
        if request.status_code == 404:
            raise RuntimeError(f"Schema URL not found: {url}")

        request.raise_for_status()
        if request.headers['Content-Type'] != 'application/json':
            raise RuntimeError('Can only use JSON schema URLs, content type of '
                               f"{url} is {request.headers['Content-Type']}")

        try:
            return request.json()
        except ValueError as error:
            raise RuntimeError(f"JSON schema at {url} was invalid") from error
    except ValueError:
        with open(path, 'r') as schema_file:
            return parse(tokens, schema_file)

def parse_documentation(session, url, data=None):
    """
    Parse a documentation wiki.
    """

    field = r"^\*\* '''[a-z_]+'''"
    tokens = {
        'special_type': {
            'pattern': re.compile(r"^\* '''([A-Z]+\([A-Za-z0-9, ]+\))"),
            'line': {
                'type': {
                    'pattern': re.compile(r"^\* '''([A-Z]+)\([A-Za-z0-9, ]+\)")
                },
                'limit': {
                    'pattern': re.compile(r".* maximum length limitation is (\d+)")
                }
            }
        },
        'table': {
            'pattern': re.compile(r"^\* '''([a-z_]+)'''"),
            'line': {
                'primary_key_combined': {
                    'pattern': re.compile(r'.* Primary key is \(([a-z_]+)(?:, ([a-z_]+))*\)')
                }
            },
            'within': {
                'field': {
                    'pattern': re.compile(r"^\*\* '''([a-z_]+)'''"),
                    'line': {
                        'type': {
                            'pattern': re.compile(field + r" - ([A-Z]+(?:\([A-Za-z0-9, ]+\))?)")
                        },
                        'primary_key': {
                            'pattern': re.compile(field + r" - [^:]*primary key:")
                        },
                        'reference': {
                            'pattern': \
                                re.compile(field + r" - [^:]*reference to ([a-z_]+)\.([a-z_]+):")
                        },
                        'null': {
                            'pattern': re.compile(field + r"[^:]*: .+ NULL")
                        }
                    }
                }
            },
            'end': re.compile(r"^$")
        }
    }

    request = session.get(url)
    if request.status_code == 404:
        logging.warning('Documentation URL not found: %s', url)
        return data

    request.raise_for_status()
    if request.headers['Content-Type'] == 'application/json':
        if data is not None:
            logging.warning('Cannot append JSON to existing data')
            return data

        return request.json()

    return parse(tokens, request.text.splitlines(), data=data)

def check_existing(one, two, key, extra=''):
    """
    Check if two dictionaries that both have a key have subdictionaries with the
    same subkeys stored for that key.

    Returns the number of violations.
    """

    missing = set(one[key].keys()) - set(two[key].keys())
    superfluous = set(two[key].keys()) - set(one[key].keys())
    violations = 0

    if missing:
        logging.warning('Missing %s%s%s: %s', key,
                        's' if len(missing) > 1 else '', extra,
                        ', '.join(missing))
        violations += len(missing)

    if superfluous:
        logging.warning('Superfluous %s%s%s: %s', key,
                        's' if len(superfluous) > 1 else '', extra,
                        ', '.join(superfluous))
        violations += len(superfluous)

    return violations

def check_equal(one, two, key, extra='', special_type=None):
    """
    Check if two dictionaries have the same value stored for a key if the first
    dictionary has that key.

    Returns the number of violations.
    """

    if key in one:
        if key not in two:
            logging.warning('Missing %s%s', key, extra)
            return 1

        first = one[key]
        second = two[key]
        while special_type is not None and first in special_type:
            if "limit" in special_type[first]:
                first = "{type}({limit})".format(**special_type[first])
            else:
                first = special_type[first]["type"]

        if isinstance(first, list):
            first = tuple(first)
        if isinstance(second, list):
            second = tuple(second)

        if first != second:
            logging.warning('%s%s does not match: %s vs. %s', key, extra,
                            first, second)
            return 1

    return 0

def check_reference(documentation, doc_field, field_text):
    """
    Check whether a field reference is valid and has the same type as the
    referent.
    """

    if 'reference' in doc_field:
        ref_table_name, ref_field_name = doc_field['reference']
        if ref_table_name not in documentation['table']:
            logging.warning('Invalid table reference %s%s',
                            ref_table_name, field_text)
            return 1

        ref_fields = documentation['table'][ref_table_name]['field']
        if ref_field_name not in ref_fields:
            logging.warning('Invalid field reference %s.%s%s',
                            ref_table_name, ref_field_name, field_text)
            return 1

        ref_field = ref_fields[ref_field_name]
        if 'type' in doc_field and 'type' in ref_field and \
            doc_field['type'] != ref_field['type']:
            logging.warning('Referenced field %s.%s with type %s does not match type %s%s',
                            ref_table_name, ref_field_name, ref_field['type'],
                            doc_field['type'], field_text)
            return 1

    return 0

def validate_schema(schema, documentation):
    """
    Compare the documentation against the database schema.
    """

    violations = check_existing(schema, documentation, 'table')

    # Aliases
    documentation['special_type'].update({
        'INT': {
            'type': 'INTEGER'
        },
        'BOOL': {
            'type': 'BOOLEAN'
        }
    })

    for table_name, table in schema['table'].items():
        if table_name not in documentation['table']:
            continue

        logging.info('Checking table %s', table_name)
        table_text = ' for table {}'.format(table_name)
        doc_table = documentation['table'][table_name]
        violations += check_equal(doc_table, table, 'primary_key_combined',
                                  table_text)
        violations += check_existing(table, doc_table, 'field', table_text)

        for field_name, field in table['field'].items():
            if field_name not in doc_table['field']:
                continue

            field_text = ' of field {} in table {}'.format(field_name,
                                                           table_name)
            doc_field = doc_table['field'][field_name]

            violations += check_equal(doc_field, field, 'type', field_text,
                                      documentation['special_type'])
            if check_equal(field, doc_field, 'null', field_text) == 0:
                violations += check_equal(doc_field, field, 'null', field_text)
            else:
                violations += 1

            violations += check_equal(field, doc_field, 'primary_key',
                                      field_text)
            if 'primary_key' in doc_table:
                if 'primary_key_combined' not in table or \
                    table['primary_key_combined'] != field_name:
                    logging.warning('Table %s does not have primary key %s',
                                    table_name, field_name)
                    violations += 1

            violations += check_reference(documentation, doc_field, field_text)

    return violations

def main():
    """
    Main entry point.
    """

    config = RawConfigParser()
    config.read("settings.cfg")
    args = parse_args(config)

    if args.username is not None:
        auth = HTTPBasicAuth(args.username, args.password)
    else:
        auth = None

    session = Session()
    session.verify = args.verify
    session.auth = auth

    documentation = parse_documentation(session, args.url.format(branch=''))
    if args.branch != 'master' and '{branch}' in args.url:
        if args.branch is None:
            branch = git.Repo('..').active_branch.name
        else:
            branch = args.branch

        branch_url = args.url.format(branch='/' + branch)
        documentation = parse_documentation(session, branch_url,
                                            data=documentation)

    schema = parse_schema(session, args.path)

    if args.export:
        with open('tables-documentation.json', 'w') as tables_file:
            json.dump(documentation, tables_file, indent=4)
        with open('tables-schema.json', 'w') as tables_file:
            json.dump(schema, tables_file, indent=4)

    violations = validate_schema(schema, documentation)

    if violations > 0:
        logging.warning('Schema violations: %d', violations)
        sys.exit(2)
    else:
        logging.info('No schema violations detected')
        sys.exit(0)

if __name__ == "__main__":
    main()
