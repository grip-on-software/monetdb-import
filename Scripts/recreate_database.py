"""
Script to destroy the database and create it again with table definitions.

All data is lost if this is run!

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
import logging
import socket
from pymonetdb.control import Control
from pymonetdb.sql.connections import Connection
from pymonetdb.exceptions import OperationalError
import requests
from requests.auth import HTTPBasicAuth

TIMEOUT = 60

def check(database: str) -> bool:
    """
    Check whether the user confirms the deletion of the database.
    """

    prompt = f'''This action wipes out the {database} database.
This is a destructive process!
Are you sure you want to delete all data from the {database} database,
and replace the {database} database with a clean state? [y/N] '''
    answer = input(prompt)

    if answer.lower() != 'y':
        return False

    return True

def parse_args(config: RawConfigParser) -> Namespace:
    """
    Parse command line arguments.
    """

    description = 'Delete all data and recreate the database'
    log_levels = ['DEBUG', 'INFO', 'WARNING', 'ERROR', 'CRITICAL']
    parser = ArgumentParser(description=description, conflict_handler='resolve')
    parser.add_argument('-h', '--hostname', nargs='?',
                        default=config.get('monetdb', 'hostname'),
                        help='host name of the monetdb database to connect to')
    parser.add_argument('-s', '--passphrase',
                        default=config.get('monetdb', 'passphrase'),
                        help='passphrase for the remote control')
    parser.add_argument('-u', '--username',
                        default=config.get('monetdb', 'username'),
                        help='username that creates the tables')
    parser.add_argument('-P', '--password',
                        default=config.get('monetdb', 'password'),
                        help='password of the user creating the tables')
    parser.add_argument('-d', '--database',
                        default=config.get('monetdb', 'database'),
                        help='database name to create into')
    parser.add_argument('-f', '--force', action='store_true', default=False,
                        help='Do not prompt for confirmation')
    parser.add_argument('-c', '--no-schema', dest='create_schema',
                        action='store_false', default=True,
                        help='Do not create schema')
    parser.add_argument('-t', '--table-import', dest='import_tables', nargs='?',
                        const='create-tables.sql', default='create-tables.sql',
                        help='Import table structure')
    parser.add_argument('-i', '--no-table-import', dest='import_tables',
                        action='store_false',
                        help='Do not import table structure')
    parser.add_argument('-k', '--keep-jenkins', dest='delete_jenkins',
                        action='store_false', default=True,
                        help='Do not delete Jenkins workspace automatically')
    parser.add_argument('--jenkins-host', dest='jenkins_host',
                        default=config.get('jenkins', 'host'),
                        help='Base URL of the Jenkins instance')
    parser.add_argument('--jenkins-job', dest='jenkins_job',
                        default=config.get('jenkins', 'job'),
                        help='Jenkins job to delete workspace from')
    parser.add_argument('--jenkins-username', dest='jenkins_username',
                        default=config.get('jenkins', 'username'),
                        help='Username to log in to Jenkins')
    parser.add_argument('--jenkins-token', dest='jenkins_token',
                        default=config.get('jenkins', 'token'),
                        help='Password or API token to log in to Jenkins')
    parser.add_argument('--jenkins-crumb', dest='jenkins_crumb',
                        action='store_true',
                        default=config.getboolean('jenkins', 'crumb'),
                        help='Request a CSRF crumb from Jenkins')
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

def delete_workspace(args: Namespace) -> None:
    """
    Delete a Jenkins workspace.
    """

    if args.jenkins_username != '' and args.jenkins_token != '':
        auth = HTTPBasicAuth(args.jenkins_username, args.jenkins_token)
    else:
        auth = None

    if args.jenkins_crumb:
        crumb_url = f'{args.jenkins_host}/crumbIssuer/api/json'
        crumb_data = requests.get(crumb_url, auth=auth, timeout=TIMEOUT).json()
        headers = {
            str(crumb_data['crumbRequestField']): str(crumb_data['crumb'])
        }
    else:
        headers = {}

    url = f'{args.jenkins_host}/job/{args.jenkins_job}/doWipeOutWorkspace'
    requests.post(url, auth=auth, headers=headers, timeout=TIMEOUT)

def main() -> None:
    """
    Main entry point.
    """

    config = RawConfigParser()
    config.read("settings.cfg")
    args = parse_args(config)

    try:
        control = Control(hostname=args.hostname, passphrase=args.passphrase)
    except socket.error as error:
        raise RuntimeError('Cannot connect, address resolution error') from error

    database = str(args.database)
    if not args.force and not check(database):
        logging.info('Canceling process due to user input.')
        return

    try:
        status = control.status(database)
        if status['state'] != 3:
            logging.info('Stoppping database...')
            control.stop(database)

        logging.info('Destroying database...')
        control.destroy(database)
    except OperationalError as error:
        logging.warning('MonetDB error: %s', str(error))
        logging.warning('Maybe the database did not exist, continuing anyway.')

    logging.info('Creating database...')
    control.create(database)

    logging.info('Starting database...')
    control.start(database)

    logging.info('Releasing database...')
    control.release(database)

    connection = Connection(database, hostname=args.hostname,
                            username=args.username, password=args.password,
                            autocommit=True)

    if args.create_schema:
        logging.info('Creating schema...')
        connection.execute('CREATE SCHEMA "gros";')
        connection.execute('SET SCHEMA "gros";')

    if args.import_tables:
        logging.info('Creating tables...')
        with open(args.import_tables, 'r', encoding='utf-8') as table_file:
            command = ""
            for line in table_file:
                if line.strip() == "":
                    continue

                command += line
                if line.rstrip().endswith(';'):
                    connection.execute(command)
                    command = ""

    connection.close()

    if args.delete_jenkins:
        logging.info('Deleting Jenkins workspace...')
        delete_workspace(args)

    logging.info('Done.')

if __name__ == "__main__":
    main()
