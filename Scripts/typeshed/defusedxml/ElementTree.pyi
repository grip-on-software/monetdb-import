from typing import AnyStr, IO, Optional
from xml.etree.ElementTree import ElementTree, XMLParser

def parse(source: IO[AnyStr], parser: Optional[XMLParser] = None, forbid_dtd: bool = False, forbid_entities: bool = True, forbid_external: bool = True) -> ElementTree: ...
def fromstring(text: str, forbid_dtd: bool = False, forbid_entities: bool = True, forbid_external: bool = True) -> ElementTree: ...
