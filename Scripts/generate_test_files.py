"""
Script to write JSON files based on schemas from the data-gathering repository.

This only performs the first steps of generating valid files for import.

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
from copy import deepcopy
from fnmatch import fnmatch
import json
import logging
from pathlib import Path
from typing import Any, Dict, Optional, Union, TYPE_CHECKING
from urllib.parse import urlsplit
from hypothesis import HealthCheck, Phase, given, settings, target
from hypothesis.errors import InvalidArgument
from hypothesis_jsonschema import from_schema
from hypothesis_jsonschema._resolve import LocalResolver, resolve_all_refs

if TYPE_CHECKING:
    from jsonschema.exceptions import RefResolutionError as _RefResolutionError
else:
    from jsonschema.exceptions import _RefResolutionError

Schema = Dict[str, Any]
SchemaExport = Union[str, Dict[str, str]]
SchemaExports = Dict[str, SchemaExport]

class LocalRemoteResolver(LocalResolver):
    """
    Resolver that fetches remote references as if they are local.
    """

    def __init__(self, base_uri: str, referrer: Schema,
                 repo_path: Optional[Path] = None, **kwargs: Any):
        super().__init__(base_uri, referrer, **kwargs)
        self.repo_path = repo_path
        parts = urlsplit(base_uri)
        self.base_site = parts.netloc # gros.liacs.nl

    def resolve_remote(self, uri: str) -> Schema: # type: ignore[override]
        parts = urlsplit(uri)
        if self.repo_path is not None and parts.netloc == self.base_site:
            path = parts.path.split('/')
            if path[-2] == 'data-gathering':
                schema_path = Path(self.repo_path, 'schema', path[-1])
            else:
                schema_path = Path(self.repo_path, 'schema', path[-2], path[-1])
            with schema_path.open('r', encoding='utf-8') as schema_file:
                document: Schema = json.load(schema_file)
            self.store[uri] = document
            return document

        raise LookupError(f"Outside of scope: {uri}")

def resolve(schema: Schema, repo_path: Path) -> Schema:
    """
    Resolve references to (other) schema files in the schema.
    """

    try:
        resolver = LocalRemoteResolver.from_schema(schema,
                                                   repo_path=repo_path)
        resolved: Schema = resolve_all_refs(schema, resolver=resolver)
        # Ensure all definitions are also resolved (for update trackers)
        for reference, subschema in schema['$defs'].items():
            resolved['$defs'][reference] = resolve_all_refs(subschema,
                                                            resolver=resolver)
        return resolved
    except (_RefResolutionError, AssertionError, RecursionError,
            InvalidArgument) as error:
        raise LookupError('Could not resolve references') from error

def parse_args() -> Namespace:
    """
    Parse command line arguments.
    """

    description = 'Generate test files from JSON schemas'
    log_levels = ['DEBUG', 'INFO', 'WARNING', 'ERROR', 'CRITICAL']
    parser = ArgumentParser(description=description)
    parser.add_argument('-r', '--repo-path',
                        default='../../../data-gathering',
                        help='Path to the data-gathering repository')
    parser.add_argument('-w', '--write-path',
                        default='../Code/importerjson',
                        help='Path to write test files to')
    parser.add_argument('-f', '--file', default=None,
                        help='Glob pattern of schema/export files to generate')
    parser.add_argument('-c', '--count', type=int, default=10,
                        help='Number of test projects to create')
    parser.add_argument('-m', '--max', type=int, default=1,
                        help='Number of examples to try to generate'
                             '(at least `--count`, more overwrites early ones)')
    parser.add_argument('-l', '--log', choices=log_levels, default='INFO',
                        help='Log level (info by default)')

    return parser.parse_args()

class Generator:
    """
    Class that handles generated data examples.
    """

    def __init__(self, write_path: Path, count: int, max_examples: int,
                 root: bool = False) -> None:
        self.path = write_path
        self.count = count
        self.max_examples = max_examples
        self.root = root
        self.current = 0
        self.total = 0

    def generate_schema(self, path: str, schema_export: SchemaExport,
                        repo_path: Path) -> None:
        """
        Generate a data file from a schema file.
        """

        logging.info('Reading schema file %s', path)
        schema_path = Path(repo_path / 'schema' / path)
        with schema_path.open('r', encoding='utf-8') as schema_file:
            try:
                schema: Schema = resolve(json.load(schema_file), repo_path)
            except LookupError:
                logging.exception('Resolving references in %s failed', path)
                return

            if isinstance(schema_export, dict):
                for reference, export in schema_export.items():
                    if reference in schema['$defs']:
                        self.generate(schema['$defs'][reference], export)
                        self.reset()
                    else:
                        logging.warning('Schema %s has no definition %s',
                                        path, reference)
            else:
                self.generate(schema, schema_export)

    def reset(self) -> None:
        """
        Reset the generator.
        """

        self.current = 0
        self.total = 0

    def generate(self, schema: Schema, filename: str) -> None:
        """
        Generate example export files from the schema and write them to test
        projects in the output path using the filename.
        """

        logging.info('Generating %d example(s) to %s', self.count, filename)

        hypothesis = from_schema(deepcopy(schema), allow_x00=False)

        @given(hypothesis)
        @settings(max_examples=max(self.count, self.max_examples),
                  phases=(Phase.generate, Phase.target),
                  database=None,
                  deadline=None,
                  suppress_health_check=(HealthCheck.filter_too_much,
                                         HealthCheck.too_slow))
        def export_example(example: Any) -> None:
            """
            Export the generated `example` to the path.
            """

            if self.test_complete:
                return

            target(len(example))
            self.current = self.current % self.count + 1
            self.total += 1
            if self.root:
                export_path = Path(self.path, filename)
            else:
                export_path = Path(self.path, f'TEST{self.current}', filename)

            logging.info('Writing to %s', export_path)
            with export_path.open('w', encoding='utf-8') as export_file:
                json.dump(example, export_file, indent=4)

        export_example() # pylint: disable=no-value-for-parameter
        if self.total < self.count:
            logging.warning('Only wrote %d examples instead of expected %d',
                            self.total, self.count)

    @property
    def test_complete(self) -> bool:
        """
        Check whether enough examples have been created.
        """

        return self.max_examples <= self.count <= self.current

def write_paths(write_path: Path, count: int) -> None:
    """
    Create export directories.
    """

    for test_count in range(1, count + 1):
        Path(write_path, 'export', f'TEST{test_count}').mkdir(parents=True,
                                                              exist_ok=True)

def match(file_pattern: str, schema_file: str,
          schema_export: SchemaExport) -> bool:
    """
    Check if the `file_pattern` matches one of the `schema_file` or
    `schema_export` filenames.
    """

    if fnmatch(schema_file, file_pattern):
        return True
    if isinstance(schema_export, dict):
        return any(fnmatch(export, file_pattern)
                   for export in schema_export.values())
    return fnmatch(schema_export, file_pattern)

def main() -> None:
    """
    Main entry point.
    """

    args = parse_args()
    logging.basicConfig(format='%(asctime)s:%(levelname)s:%(message)s',
                        level=getattr(logging, args.log.upper(), None))

    repo_path = Path(args.repo_path)
    write_path = Path(args.write_path)
    write_paths(write_path, args.count)

    exports_path = Path(repo_path / 'schema-exports.json')
    with exports_path.open('r', encoding='utf-8') as exports_file:
        schema_exports: SchemaExports = json.load(exports_file)

    for path, schema_export in schema_exports.items():
        if args.file is not None and not match(args.file, path, schema_export):
            continue

        generator = Generator(write_path / 'export', args.count, args.max)
        generator.generate_schema(path, schema_export, repo_path)

    root_files: SchemaExports = {
        'vcsdev_to_dev.json': 'data_vcsdev_to_dev.json'
    }
    for path, schema_export in root_files.items():
        if args.file is not None and not match(args.file, path, schema_export):
            continue

        generator = Generator(write_path, 1, args.max, root=True)
        generator.generate_schema(path, schema_export, Path('..'))

if __name__ == '__main__':
    main()
