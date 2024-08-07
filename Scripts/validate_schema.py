"""
Script to validate a documentation page against the database schema.

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
from collections import OrderedDict
from copy import deepcopy
from functools import total_ordering
import io
import json
import logging
from pathlib import Path
import re
import sys
from typing import Any, Dict, Generator, IO, Iterable, List, Mapping, \
    Optional, Set, Tuple, Union
from urllib.parse import urlparse
from xml.etree.ElementTree import Element
import zipfile
import defusedxml.ElementTree
import regex
from requests import Session
from requests.auth import HTTPBasicAuth

Token = Dict[str, Any]
Tokens = Dict[str, Token]
Schema = Dict[str, Any]
Field = Union[str, List[Optional[str]]]
TableField = Dict[str, Union[List[str], Tuple[str, ...], str]]

# Tokens for SQL schema file
SQL_IDENT = r"[a-z_]+"
SQL_IDENT_LIST = rf'\("({SQL_IDENT})"(?:,\s*"({SQL_IDENT})")*\)'
SQL_FIELD = rf'^\s*"{SQL_IDENT}"'
SQL_FIELD_TYPE = rf'{SQL_FIELD}\s+[A-Z]+(?:\([0-9,]+\))?'
SQL_TOKENS = {
    'table': {
        'pattern': regex.compile(r'^CREATE TABLE "[a-z_]+"."([a-z_]+)" \('),
        'within': {
            'field': {
                'pattern': regex.compile(r'^\s*"([a-z_]+)"'),
                'line': {
                    'type': {
                        'pattern': regex.compile(rf'''{SQL_FIELD}\s+([A-Z]+
                                                      (?:\([0-9,]+\))?)''',
                                                 re.X)
                    },
                    'null': {
                        'pattern': regex.compile(rf'{SQL_FIELD_TYPE}\s*NULL')
                    },
                    'primary_key': {
                        'pattern': regex.compile(rf'''{SQL_FIELD_TYPE}.*\s
                                                      AUTO_INCREMENT''', re.X)
                    }
                }
            },
            'primary_key_combined': {
                'pattern': regex.compile(rf'''^\s*CONSTRAINT\s"[a-z_]+"\s
                                              PRIMARY\sKEY\s*
                                              {SQL_IDENT_LIST}''', re.X)
            }
        },
        'end': regex.compile(r"^\);")
    }
}

# Tokens for MySQL Workbench XML schemas
MWB_TOKENS = {
    'table': {
        'selector': {
            'struct-name': 'db.mysql.Table'
        },
        'line': {
            'name': 'name'
        },
        'within': {
            'field': {
                'selector': {
                    'struct-name': 'db.mysql.Column'
                },
                'line': {
                    'name': 'name',
                    'primary_key': {
                        'selector': {
                            'key': 'autoIncrement'
                        },
                        'filter': '1',
                        'mapping': {
                            '1': True,
                            '0': False
                        }
                    },
                    'type': {
                        'selector': {'key': 'simpleType'},
                        'mapping': {
                            'com.mysql.rdbms.mysql.datatype.date': 'DATE',
                            'com.mysql.rdbms.mysql.datatype.decimal': 'DECIMAL',
                            'com.mysql.rdbms.mysql.datatype.float': 'FLOAT',
                            'com.mysql.rdbms.mysql.datatype.int': 'INTEGER',
                            'com.mysql.rdbms.mysql.datatype.text': 'TEXT',
                            'com.mysql.rdbms.mysql.datatype.timestamp_f':
                                'TIMESTAMP',
                            'com.mysql.rdbms.mysql.datatype.tinyint': 'BOOLEAN',
                            'com.mysql.rdbms.mysql.datatype.varchar': 'VARCHAR'
                        }
                    },
                    'limit': 'length',
                    'precision': 'precision',
                    'scale': 'scale',
                    'null': {
                        'selector': {'key': 'isNotNull'},
                        'filter': '0',
                        'mapping': {
                            '1': False,
                            '0': True
                        }
                    }
                }
            },
            'primary_key_combined': {
                'unroll_prefix': '',
                'selector': {'struct-name': 'db.mysql.Index'},
                'filter': {
                    'key': 'indexType',
                    '.': 'PRIMARY'
                },
                'within': {
                    'index': {
                        'selector': {'struct-name': 'db.mysql.IndexColumn'},
                        'line': {'reference': 'referencedColumn'}
                    }
                }
            },
            'reference': {
                'selector': {'struct-name': 'db.mysql.ForeignKey'},
                'within': {
                    'from': {
                        'selector': {'key': 'columns'},
                        'unroll_prefix': True,
                        'line': {
                            'reference': {
                                'selector': {'type': 'object'}
                            }
                        }
                    },
                    'to': {
                        'selector': {'key': 'referencedColumns'},
                        'unroll_prefix': True,
                        'line': {
                            'reference': {
                                'selector': {'type': 'object'}
                            }
                        }
                    }
                }
            }
        }
    }
}

# Tokens for Markdown documentation
MD_TYPE = r"^-\s+\*\*[A-Z]+\([A-Za-z0-9, ]+\)"
MD_FIELD = r"\s+-\s+\*\*[a-z_]+\*\*"
MD_TOKENS = {
    'special_type': {
        'pattern': regex.compile(r"^- +\*\*([A-Z]+\([A-Za-z0-9, ]+\))"),
        'multiline': True,
        'line': {
            'type': {
                'pattern': regex.compile(r"^- +\*\*([A-Z]+)\([A-Za-z0-9, ]+\)")
            },
            'limit': {
                'pattern': regex.compile(r""".*\s+maximum\s+length\s+
                                             limitation\s+is\s+(\d+)""",
                                         re.S | re.X)
            }
        },
        'end': regex.compile(r"^- +\*\*[A-Z]+\([A-Za-z0-9, ]+\)")
    },
    'group': {
        'pattern': regex.compile(r"^## (.+ \(.+\))$"),
        'gobble': 'table'
    },
    'table': {
        'pattern': regex.compile(r"^- +\*\*([a-z_]+)\*\*"),
        'multiline': True,
        'line': {
            'continuation': {
                'data': False,
                'pattern': regex.compile(rf"""^-\s+\*\*([a-z_]+)\*\*
                                              (?!.+^{MD_FIELD})""",
                                         re.M | re.S | re.X)
            },
            'primary_key_combined': {
                'pattern': regex.compile(r'''.*\s+Primary\s+key\s+
                                             (?:is|consists\s+of)\s+\(
                                             ([a-z_]+)(?:,\s+([a-z_]+))*\)''',
                                         re.S | re.X)
            }
        },
        'within': {
            'field': {
                'pattern': regex.compile(r".*^ +- +\*\*([a-z_]+)\*\*",
                                         re.M | re.S),
                'multiline': True,
                'line': {
                    'type': {'pattern': regex.compile(
                        rf"""(?:(?!^{MD_FIELD}).+)?^{MD_FIELD}\s+-\s+([A-Z]+
                             (?:\([A-Za-z0-9, ]+\))?)(?!.+^{MD_FIELD})""",
                        re.M | re.S | re.X
                    )},
                    'primary_key': {'pattern': regex.compile(
                        rf"""(?:(?!^{MD_FIELD}).+)?^{MD_FIELD}\s+-\s+[^:]*
                             primary\s+key[:-](?!.+^{MD_FIELD})""",
                        re.M | re.S | re.X
                    )},
                    'reference': {'pattern': regex.compile(
                        rf"""(?:(?!^{MD_FIELD}).+)?^{MD_FIELD}\s+-\s+[^:]*
                             reference\s+to\s+([a-z_]+)\.([a-z_]+)
                             (?:\s+or\s+([a-z_]+)\.([a-z_]+))*:
                             (?!.+^{MD_FIELD})""", re.M | re.S | re.X
                    )},
                    'null': {'pattern': regex.compile(
                        rf"""(?:(?!^{MD_FIELD}).+)?^{MD_FIELD}[^:]*:\s+.+\s+
                             NULL(?!.+^{MD_FIELD})""", re.M | re.S | re.X
                    )},
                },
                'end': regex.compile(rf".*^{MD_FIELD}.*^{MD_FIELD}|.*\n\n",
                                     re.M | re.S)
            }
        },
        'end': regex.compile(r".*\n\n|^\n?$", re.S)
    }
}

PRIMARY = 'primary_key'
COMBINED = 'primary_key_combined'

def parse_args(config: RawConfigParser) -> Namespace:
    """
    Parse command line arguments.
    """

    description = 'Delete all data and recreate the database'
    log_levels = ['DEBUG', 'INFO', 'WARNING', 'ERROR', 'CRITICAL']
    verify: Union[str, bool] = config.get('schema', 'verify', fallback=True)
    if verify in ('false', 'no', 'off', '-', '0', ''):
        verify = False
    elif not Path(str(verify)).exists():
        verify = True

    parser = ArgumentParser(description=description)
    parser.add_argument('--path',
                        default=config.get('schema', 'path',
                                           fallback='create-tables.sql'),
                        help='Path to retrieve SQL schema from')
    parser.add_argument('--doc',
                        default=config.get('schema', 'doc',
                                           fallback='Database_structure.md'),
                        help='Path to retrieve Markdown documentation from')
    parser.add_argument('--url',
                        default=config.get('schema', 'url', fallback=''),
                        help='URL to retrieve JSON documentation from')
    parser.add_argument('--verify', nargs='?', const=True, default=verify,
                        help='Enable SSL certificate verification')
    parser.add_argument('--no-verify', action='store_false', dest='verify',
                        help='Disable SSL certificate verification')
    parser.add_argument('--username', help='Username for documentation URL',
                        default=config.get('schema', 'username', fallback=''))
    parser.add_argument('--password', help='Password for documentation URL',
                        default=config.get('schema', 'password', fallback=''))
    parser.add_argument('-l', '--log', choices=log_levels, default='INFO',
                        help='log level (info by default)')
    parser.add_argument('--export', action='store_true', default=False,
                        help='Write JSON files of extracted schemas')

    args = parser.parse_args()
    logging.basicConfig(format='%(asctime)s:%(levelname)s:%(message)s',
                        level=getattr(logging, args.log.upper(), None))

    return args

class SchemaParser:
    """
    Token-based parser of documented schemas.
    """

    def __init__(self, tokens: Tokens) -> None:
        self._current_tokens = tokens
        self._data: Schema = {}
        self._parent_data = self._data

    def handle_field(self, name: str, token: Token, field: Field) -> bool:
        """
        Handle a nested field.
        """

        raise NotImplementedError('Must be implemented by subclasses')

    def parse(self, line_iterator: Iterable[str]) -> Schema:
        """
        Parse a file, sequence or other iterable that provides lines.
        """

        raise NotImplementedError('Must be implemented by subclasses')

class LineParser(SchemaParser):
    """
    Token-based documentation parser which reads line by line, possibly with
    additional line context storage.
    """

    def __init__(self, tokens: Tokens, single_line: bool = True) -> None:
        super().__init__(tokens)

        self._single_line = single_line
        self._multiline: Optional[bool] = False
        self._reverse = False
        self._previous = ""
        self._gobble: Optional[Dict[str, Any]] = None

    def handle_field(self, name: str, token: Token, field: Field) -> bool:
        if name == '__end':
            self._current_tokens = token['old']
            old_name = token.pop('old_token')
            self._current_tokens[old_name].pop('__skip_line', None)
            self._current_tokens[old_name].pop('__skip_within', None)
            self._parent_data = token.pop('old_data', self._data)
            self._multiline = None
            return True

        # Only use the 'parent' match if we haven't matched it before for the
        # multi-line
        if '__skip_line' not in token and '__skip_within' not in token:
            if len(field) > 1:
                self._parent_data[name] = field
            elif not field:
                self._parent_data[name] = True
            elif 'within' in token or 'line' in token:
                self._parent_data.setdefault(name, {})
                self._parent_data[name][field[0]] = {}

                if self._gobble is not None and name == self._gobble['token']:
                    self._gobble['field'].append(field[0])
            elif 'gobble' in token:
                self._parent_data.setdefault(name, {})
                self._parent_data[name][field[0]] = []
                self._gobble = {
                    'token': token['gobble'],
                    'field': self._parent_data[name][field[0]]
                }
            else:
                self._parent_data[name] = field[0]

        if 'line' in token or 'within' in token:
            old_current_tokens = deepcopy(self._current_tokens)
            if 'line' in token and '__skip_line' not in token:
                self._current_tokens = OrderedDict(token['line'])
                old_current_tokens[name]['__skip_line'] = True
            elif 'within' in token and '__skip_within' not in token:
                self._current_tokens = OrderedDict(token['within'])
                old_current_tokens[name]['__skip_within'] = True

            self._current_tokens['__end'] = {
                'pattern': token.get('end'),
                'old': old_current_tokens,
                'old_data': self._parent_data,
                'old_token': name
            }

            if self._single_line or \
                ('__skip_line' not in token and '__skip_within' not in token):
                self._parent_data = self._parent_data[name][field[0]]

            self._multiline = token.get('multiline', False)
            return True

        return False

    def test_line(self, line: str) -> List[str]:
        """
        Test whether the current token scope match the given line.
        """

        matches = []
        iterator = reversed(self._current_tokens.items()) if self._reverse \
            else self._current_tokens.items()

        for name, token in iterator:
            if token['pattern'] is None:
                # End token for a single line, handled by parse afterward
                continue

            match = token['pattern'].match(self._previous + line)
            if match:
                if not token.get('data', True):
                    matches.append('__data_continuation')
                    continue

                matches.append(name)
                groups: List[Optional[str]] = []
                for index in range(len(match.groups())):
                    captures = match.captures(index + 1)
                    if captures:
                        groups.extend(captures)
                    else:
                        groups.append(None)

                result = self.handle_field(name, token, groups)
                if result:
                    return matches

        self._multiline = None
        return matches

    def _update_multi_line(self, line: str, matches: List[str],
                           old_token: Token) -> None:
        if '__end' in matches:
            # The '__end' token triggered and handled most cleanup already
            self._previous = ""
        else:
            # Remove any former lines not relevant for the token pattern
            self._multiline = True
            previous = self._previous
            while previous and \
                (old_token['end'].match(previous + line) or \
                not old_token['pattern'].match(previous + line)):
                previous = '\n'.join(previous.split('\n')[1:])
            self._previous = previous + line

    def handle_line_end(self, line: str, matches: List[str]) -> Optional[bool]:
        """
        Handle matches for a line when there are no nesting matches.
        """

        self._reverse = True
        end_token = self._current_tokens['__end']
        name = end_token['old_token']
        old_token = end_token['old'][name]
        # Try the same line if we're in a 'line' group but there's also another
        # 'within' group for the old token
        if '__data_continuation' not in matches and 'within' in old_token and \
            '__skip_within' not in old_token:
            self._current_tokens = end_token['old']
            return True

        # We matched something but the old token pattern doesn't match
        if matches and end_token['pattern'] is not None and \
            not end_token['pattern'].match(self._previous + line):
            # Nothing more to match for this line in single-line mode
            if self._single_line:
                return False

            self._update_multi_line(line, matches, old_token)
            return True

        if 'within' in old_token and '__skip_within' in old_token and \
            'end' in old_token and \
            not old_token['end'].match(self._previous + line):
            # Only go to parent when the '__end' token triggers
            # For now go to the next line and start matching
            if old_token.get('multiline'):
                self._previous = ""
            return False

        # Go to parent tokens and clean up if necessary
        self._current_tokens = end_token['old']
        self._current_tokens[name].pop('__skip_line', None)
        self._current_tokens[name].pop('__skip_within', None)
        self._parent_data = end_token.pop('old_data', self._data)
        if self._single_line:
            return False

        self._previous = ""
        self._reverse = False
        return False if not matches else None

    def parse(self, line_iterator: Iterable[str]) -> Schema:
        self._previous = ""
        for line in line_iterator:
            first = True
            # This is a hack, but when the iterator is reversed so that the end
            # pattern is matched first, then overwriting earlier field values
            # may be prevented, but it needs to be unreversed for simpler
            # situations so that the fields match in the first place...
            while first or not self._multiline:
                first = False
                matches = self.test_line(line)
                if self._multiline and self._reverse:
                    self._multiline = False

                if self._multiline is None:
                    if '__end' in self._current_tokens:
                        # No nesting matches
                        loop = self.handle_line_end(line, matches)
                        if loop:
                            continue
                        if loop is not None:
                            break
                    else:
                        self._parent_data = self._data
                        break

                if self._multiline:
                    self._previous += line

            if not self._multiline:
                self._previous = ""

        return self._data

@total_ordering
class SymbolicRef:
    """
    Symbolic reference to another named object.
    """

    def __init__(self, identifier: str, references: Mapping[str, str]):
        self._id = identifier
        self._references = references

    @property
    def reference(self) -> str:
        """
        Retrieve the reference ID.
        """

        return self._id

    def __str__(self) -> str:
        return self._references.get(self._id, self._id)

    def __repr__(self) -> str:
        return repr(str(self))

    def __hash__(self) -> int:
        return hash(str(self))

    def __eq__(self, other: object) -> bool:
        return str(self) == str(other)

    def __lt__(self, other: object) -> bool:
        return str(self) < str(other)

class XMLParser(SchemaParser):
    """
    Parser for XML files.
    """

    def __init__(self, tokens: Tokens) -> None:
        super().__init__(tokens)
        self._references: Dict[str, str] = {}

    def handle_field(self, name: str, token: Token, field: Field) -> bool:
        if 'filter' in token and field != token['filter']:
            return False

        if 'mapping' in token:
            self._parent_data[name] = token['mapping'].get(field)
            return True

        if name == 'reference':
            self._parent_data[name] = [
                SymbolicRef(reference, self._references)
                for reference in field if reference is not None
            ]
            return True

        if field != "-1":
            self._parent_data[name] = field
            return True

        return False

    @staticmethod
    def _get_path(selector: Dict[str, str], nesting: str = '//',
                  tag: str = 'value') -> str:
        selectors = ''.join(
            f"[{'@' if key != '.' else ''}{key}='{value}']"
            for key, value in selector.items()
        )
        return f".{nesting}{tag}{selectors}"

    @classmethod
    def _flatten(cls, data: Iterable[Union[List[str], Dict[str, str], str]],
                 prefix: Union[bool, str] = True) -> Generator[str, None, None]:
        for element in data:
            if isinstance(element, list):
                yield from cls._flatten(element, prefix=prefix)
            elif isinstance(element, dict):
                yield from cls._flatten(element.values(), prefix=prefix)
            elif isinstance(prefix, str):
                reference = str(element)
                yield f"{prefix}{reference[reference.find('.') + 1:]}"
            else:
                yield element

    def parse_line(self, element: Element, tokens: Tokens) -> None:
        """
        Parse elements in a field.
        """

        for name, token in tokens.items():
            if isinstance(token, str):
                token = {'selector': {'key': token}}

            path = self._get_path(token['selector'], nesting='/', tag='*')
            children = element.findall(path)
            if len(children) == 0:
                continue

            if 'key' in token['selector'] and name != 'reference':
                field: Field = str(children[0].text)
            else:
                field = [child.text for child in children]

            self.handle_field(name, token, field)

    def parse_nested(self, element: Element, tokens: Tokens,
                     data: Dict[str, Any], prefix: str = '') -> None:
        """
        Parse nested elements.
        """

        for name, token in tokens.items():
            data.setdefault(name, [])

            path = self._get_path(token['selector'])
            filter_path = self._get_path(token['filter']) if 'filter' in token \
                else None
            named = False
            for child in element.findall(path):
                if filter_path is not None and child.find(filter_path) is None:
                    continue

                data[name].append({})
                old_data = self._parent_data
                self._parent_data = data[name][-1]

                if 'line' in token:
                    self.parse_line(child, token['line'])

                name_prefix = ''
                if 'name' in self._parent_data and 'id' in child.attrib:
                    # Create fully qualified names for references
                    named = True
                    self._references[child.attrib['id']] = \
                        f"{prefix}{self._parent_data['name']}"
                    if prefix == '':
                        name_prefix = f"{self._parent_data['name']}."

                if 'within' in token:
                    self.parse_nested(child, token['within'], data[name][-1],
                                      prefix=name_prefix)

                self._parent_data = old_data

            if 'unroll_prefix' in token:
                data[name] = list(self._flatten(data[name],
                                                prefix=token['unroll_prefix']))
            elif named:
                data[name] = {
                    child_data['name']: child_data for child_data in data[name]
                }

    def parse(self, line_iterator: Union[Iterable[str], IO[bytes]]) -> Schema:
        if isinstance(line_iterator, (io.IOBase, IO)):
            root = defusedxml.ElementTree.parse(line_iterator).getroot()
        else:
            root = defusedxml.ElementTree.fromstring("\n".join(line_iterator))

        self.parse_nested(root, self._current_tokens, self._data)

        return self._data

def parse_schema(session: Session, path: str) -> Schema:
    """
    Parse an SQL table schema or retrieve an already-parsed JSON structure.
    """

    try:
        url = urlparse(path)
        if url.scheme == '' or url.netloc == '':
            raise ValueError('URL must be absolute')

        request = session.get(path)
        request.raise_for_status()
        if request.headers['Content-Type'] != 'application/json':
            raise RuntimeError('Schema URL must have JSON content type, but '
                               f"{url} is {request.headers['Content-Type']}")

        try:
            return request.json()
        except ValueError as error:
            raise RuntimeError(f"JSON schema at {url} was invalid") from error
    except ValueError:
        if zipfile.is_zipfile(path):
            xml_parser = XMLParser(MWB_TOKENS)
            with zipfile.ZipFile(path, 'r') as zip_file:
                with zip_file.open('document.mwb.xml') as workbench_file:
                    return xml_parser.parse(workbench_file)
        else:
            line_parser = LineParser(SQL_TOKENS)
            with open(path, 'r', encoding='utf-8') as schema_file:
                return line_parser.parse(schema_file)

def parse_documentation(session: Session, url: str, path: Path) -> Schema:
    """
    Parse a documentation file or JSON structure from a URL.
    """

    if path.is_file():
        md_parser = LineParser(MD_TOKENS, single_line=False)
        with path.open('r', encoding='utf-8') as structure_file:
            return md_parser.parse(structure_file)

    request = session.get(url)
    request.raise_for_status()
    if request.headers['Content-Type'] != 'application/json':
        raise RuntimeError('Documentation URL must have JSON content type, '
                           f"but {url} is {request.headers['Content-Type']}")

    return request.json()

def check_existing(one: Dict[str, Any], two: Dict[str, Any], key: str,
                   extra: str = '') -> int:
    """
    Check if two dictionaries that both have a key have subdictionaries with the
    same subkeys stored for that key. Returns the number of violations.
    """

    if key not in one or key not in two:
        logging.warning('Missing %s%s', key, extra)
        return 1

    missing: Set[str] = set(one[key].keys()) - set(two[key].keys())
    superfluous: Set[str] = set(two[key].keys()) - set(one[key].keys())
    violations = 0
    if missing:
        logging.warning('Missing %d %s%s%s: %s', len(missing), key,
                        's' if len(missing) > 1 else '', extra,
                        ', '.join(missing))
        violations += len(missing)
    if superfluous:
        logging.warning('Superfluous %d %s%s%s: %s', len(superfluous), key,
                        's' if len(superfluous) > 1 else '', extra,
                        ', '.join(superfluous))
        violations += len(superfluous)

    return violations

def _convert_type(field: TableField, key: str,
                  specials: Optional[Dict[str, Dict[str, str]]]) -> List[str]:
    item = field[key]
    if specials is not None:
        if 'limit' in field:
            item = f"{item}({field['limit']})"
        elif 'precision' in field and 'scale' in field:
            item = f"{item}({field['precision']},{field['scale']})"

    while specials is not None and item in specials:
        if "type" not in specials[str(item)]:
            raise ValueError(f'Missing type for special_type {item} of {key}')

        if "limit" in specials[str(item)]:
            next_type = specials[str(item)]
            item = f"{next_type['type']}({next_type['limit']})"
        else:
            item = specials[str(item)]["type"]

    if isinstance(item, tuple):
        return list(item)
    if isinstance(item, list):
        return item
    return [item]

def check_equal(one: TableField, two: TableField, key: str, extra: str = '',
                specials: Optional[Dict[str, Dict[str, str]]] = None) -> int:
    """
    Check if two dictionaries have the same value stored for a key if the first
    dictionary has that key. Returns the number of violations.
    """

    if key not in one:
        return 0
    if key not in two:
        logging.warning('Missing %s%s', key, extra)
        return 1

    try:
        first = _convert_type(one, key, specials)
        second = _convert_type(two, key, specials)
    except ValueError as error:
        logging.warning('%s%s', error, key)
        return 1
    if first != second:
        logging.warning('Unmatched %s%s: %s vs. %s', key, extra, first, second)
        return 1
    return 0

def _check_single_reference(ref_table_name: str, ref_field_name: str,
                            documentation: Schema, doc_field: Dict[str, Any],
                            field_text: str) -> int:
    # Check a single reference. Returns 1 if there is a violation, otherwise 0.
    if 'field' not in documentation['table'].get(ref_table_name, {}):
        logging.warning('Invalid table reference %s%s', ref_table_name, field_text)
        return 1

    ref_fields = documentation['table'][ref_table_name]['field']
    if ref_field_name not in ref_fields:
        logging.warning('Invalid field reference %s.%s%s',
                        ref_table_name, ref_field_name, field_text)
        return 1

    ref_type = ref_fields[ref_field_name].get('type')
    if 'type' in doc_field and ref_type is not None and \
        doc_field['type'] != ref_type:
        logging.warning('Referenced field %s.%s with type %s does not match type %s%s',
                        ref_table_name, ref_field_name, ref_type,
                        doc_field['type'], field_text)
        return 1

    return 0

def _check_reference(table_name: str, field_name: str, table: Dict[str, Any],
                     documentation: Schema, doc_field: Dict[str, Any]) -> int:
    # Check whether a field reference is valid and has the same type as the
    # referent. Returns the number of violations
    field_text = f' of field {field_name} in table {table_name}'
    from_name = f"{table_name}.{field_name}"
    references = set()
    violations = 0
    if 'reference' in doc_field:
        reference: List[Optional[str]] = doc_field['reference']
        while reference and reference[0] is not None and reference[1] is not None:
            violations += _check_single_reference(reference[0], reference[1],
                                                  documentation, doc_field,
                                                  field_text)
            if 'reference' in table:
                to_name = f"{reference[0]}.{reference[1]}"
                references.add(to_name)
                if not any(candidate for candidate in table['reference']
                           if from_name in candidate['from'] and
                           to_name in candidate['to']):
                    logging.warning('Superfluous reference to %s%s',
                                    to_name, field_text)
                    violations += 1

            reference = reference[2:]

    if 'reference' in table:
        violations += _check_table_reference(table_name, field_name,
                                             table['reference'], references)
    return violations

def _check_table_reference(table_name: str, field_name: str,
                           reference: List[Dict[str, List[str]]],
                           references: Set[str]) -> int:
    # Validate documentation references to MWB references from/to names pairs.
    # Returns 1 if a violation is detected, otherwise 0.
    field_text = f' of field {field_name} in table {table_name}'
    from_name = f"{table_name}.{field_name}"
    for relationship in reference:
        if not references.isdisjoint(relationship['to']) and \
            from_name in relationship['from']:
            to_reference = references.intersection(relationship['to']).pop()
            index = relationship['to'].index(to_reference)
            if relationship['from'][index] != from_name:
                logging.warning('Missing reference to %s%s', to_reference,
                                field_text)
                return 1
    return 0

def _check_field(table_name: str, field_name: str, table: Dict[str, Any],
                 documentation: Schema, doc_table: Dict[str, Any]) -> int:
    # Check a table field. Returns the number of violations.
    if field_name not in doc_table.get('field', {}):
        return 0

    field_text = f' of field {field_name} in table {table_name}'
    field: Dict[str, Any] = table['field'][field_name]
    doc_field: Dict[str, Any] = doc_table['field'][field_name]

    violations = check_equal(doc_field, field, 'type', field_text,
                             documentation['special_type'])
    if check_equal(field, doc_field, 'null', field_text) == 0:
        violations += check_equal(doc_field, field, 'null', field_text)
    else:
        violations += 1

    violations += check_equal(field, doc_field, PRIMARY, field_text)
    is_combined = field_name in table.get(COMBINED, [])
    if PRIMARY in doc_field:
        if PRIMARY not in field and not is_combined:
            logging.warning('Table %s does not have primary key %s',
                            table_name, field_name)
            violations += 1
    elif is_combined and field_name not in doc_table.get(COMBINED, []):
        logging.warning('Table %s should have primary key %s',
                        table_name, field_name)
        violations += 1

    return violations + _check_reference(table_name, field_name, table,
                                         documentation, doc_field)

def validate_schema(schema: Schema, documentation: Schema) -> int:
    """
    Compare `schema` with `documentation`. Returns the number of violations.
    """

    violations = check_existing(schema, documentation, 'table')
    if 'table' not in documentation:
        return violations

    if 'special_type' not in documentation:
        logging.warning('Missing special types in documentation')
        return violations + 1
    documentation['special_type'].update({
        'INT': {'type': 'INTEGER'},
        'BOOL': {'type': 'BOOLEAN'}
    })

    for table_name, table in schema['table'].items():
        if table_name in documentation['table']:
            logging.info('Checking table %s', table_name)
            table_text = f' for table {table_name}'
            doc_table = documentation['table'][table_name]
            violations += check_equal(doc_table, table, COMBINED, table_text)
            violations += check_existing(table, doc_table, 'field', table_text)
            for field_name in table.get('field', {}):
                violations += _check_field(table_name, field_name, table,
                                           documentation, doc_table)
    return violations

def serialize_ref(json_object: SymbolicRef) -> str:
    """
    JSON object serializer for symbolic references.
    """

    if isinstance(json_object, SymbolicRef):
        return str(json_object)

    raise TypeError(f'Object of type {type(json_object)} is not JSON serializable')

def main() -> int:
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

    documentation = parse_documentation(session, args.url.format(branch=''),
                                        Path(args.doc))
    schema = parse_schema(session, args.path)
    if args.export:
        for name, table in [('documentation', documentation), ('schema', schema)]:
            tables_path = Path(f'tables-{name}.json')
            with tables_path.open('w', encoding='utf-8') as tables_file:
                json.dump(table, tables_file, indent=4, default=serialize_ref)

    violations = validate_schema(schema, documentation)
    if violations > 0:
        logging.warning('Schema violations: %d', violations)
        return 2

    logging.info('No schema violations detected')
    return 0

if __name__ == "__main__":
    sys.exit(main())
