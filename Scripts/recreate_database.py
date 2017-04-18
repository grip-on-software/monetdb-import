"""
Script to destroy the database and create it again with table definitions.

All data is lost if this is run!
"""

from argparse import ArgumentParser
from ConfigParser import RawConfigParser
import logging
import socket
from pymonetdb.control import Control
from pymonetdb.sql.connections import Connection
from pymonetdb.exceptions import OperationalError
import requests

def check():
    """
    Check whether the user confirms the action.
    """

    answer = raw_input('Are you sure you want to delete all data and recreate the database? [y/N] ')

    if answer.lower() != 'y':
        return False

    return True

def parse_args(config):
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
    parser.add_argument('-k', '--keep-jenkins', dest='delete_jenkins',
                        action='store_false', default=True,
                        help='Do not delete Jenkins workspace automatically')
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

def main():
    """
    Main entry point.
    """

    config = RawConfigParser()
    config.read("settings.cfg")
    args = parse_args(config)

    try:
        control = Control(hostname=args.hostname, passphrase=args.passphrase)
    except socket.error as error:
        raise RuntimeError('Cannot connect, address resolution error: {}'.format(error.strerror))

    if not check():
        return

    try:
        status = control.status(args.database)
        if status['state'] != 3:
            logging.info('Stoppping database...')
            control.stop(args.database)

        logging.info('Destroying database...')
        control.destroy(args.database)
    except OperationalError as error:
        logging.warning('MonetDB error: %s', error.message)
        logging.warning('Maybe the database did not exist, continuing anyway.')

    logging.info('Creating database...')
    control.create(args.database)

    logging.info('Starting database...')
    control.start(args.database)

    logging.info('Releasing database...')
    control.release(args.database)

    connection = Connection(args.database, hostname=args.hostname,
                            username=args.username, password=args.password,
                            autocommit=True)

    logging.info('Creating schema and tables...')
    connection.execute('CREATE SCHEMA "gros";')
    connection.execute('SET SCHEMA "gros";')
    with open('create-tables.sql', 'r') as table_file:
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
        url = config.get('jenkins', 'url') + 'doWipeOutWorkspace'
        requests.post(url)

    logging.info('Done.')

if __name__ == "__main__":
    main()
