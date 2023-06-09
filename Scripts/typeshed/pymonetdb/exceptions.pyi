# Stubs for pymonetdb.exceptions (Python 3)
#
# NOTE: This dynamically typed stub was automatically generated by stubgen.

StandardError = Exception

class Warning(StandardError): ...
class Error(StandardError): ...
class InterfaceError(Error): ...
class DatabaseError(Error): ...
class DataError(DatabaseError): ...
class OperationalError(DatabaseError): ...
class IntegrityError(DatabaseError): ...
class InternalError(DatabaseError): ...
class ProgrammingError(DatabaseError): ...
class NotSupportedError(DatabaseError): ...
