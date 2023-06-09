# Stubs for pymonetdb (Python 3)
#
# NOTE: This dynamically typed stub was automatically generated by stubgen.

from pymonetdb.sql.pythonize import *
from pymonetdb.exceptions import *
from pymonetdb.sql.connections import Connection as Connection
from typing import Any

apilevel: str
threadsafety: int
paramstyle: str

def connect(*args: Any, **kwargs: Any) -> Connection: ...

# Names in __all__ with no definition:
#   BINARY
#   Binary
#   DATE
#   DBAPISet
#   DataError
#   DatabaseError
#   Date
#   DateFromTicks
#   Error
#   FIELD_TYPE
#   IntegrityError
#   InterfaceError
#   InternalError
#   MySQLError
#   NULL
#   NUMBER
#   NotSupportedError
#   OperationalError
#   ProgrammingError
#   ROWID
#   STRING
#   Set
#   TIME
#   TIMESTAMP
#   Time
#   TimeFromTicks
#   Timestamp
#   TimestampFromTicks
#   Warning
#   connections
#   constants
#   cursors
#   debug
#   escape
#   escape_dict
#   escape_sequence
#   escape_string
#   get_client_info
#   string_literal
#   version_info
