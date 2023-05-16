"""
Script to generate different workbench diagrams displaying a subpart of a model.

Copyright 2017-2020 ICTU
Copyright 2017-2022 Leiden University
Copyright 2017-2023 Leon Helwerda

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

import json
from pathlib import Path
try:
    import grt
except ImportError as error:
    raise ImportError("Can only run this in MySQL Workbench Scripting Shell") from error

def main():
    """
    Generate exports of diagrams based on groups from schema documentation.
    """

    # Use data exported from validate_schema.py
    # The JSON must exist alongside the MWB file
    path = Path(grt.root.wb.docPath).parent
    with open(path / "tables-documentation.json", encoding="utf-8") as tables_file:
        schema = json.load(tables_file)

    diagram = grt.root.wb.doc.physicalModels[0].diagrams[0]
    colors = [
        "#e69f00", "#009e73", "#d55e00", "#0072b2", "#cc79a7", "#f0e442",
        "#8c8c8c", "#56b4e9", "#8f34eb", "#9df07a", "#ffffff", "#d2d2d2"
    ]
    max_group = ""
    for index, group in enumerate(sorted(schema["group"].keys())):
        tables = schema["group"][group]
        color = colors[index % len(colors)]
        for table in diagram.rootLayer.figures:
            if table.table.name in tables:
                table.color = color
        if len(tables) > len(schema["group"].get(max_group, [])):
            max_group = group

    # Save colors
    grt.modules.Workbench.saveModel()
    max_tables = set(schema["group"][max_group])

    for group, tables in schema["group"].items():
        # Reopen model so that any changes from other groups are rolled back
        # This is annoying as it makes a dialog pop up
        grt.modules.Workbench.openModel(grt.root.wb.docPath)
        # Determine a proper name of the group to write a file for
        name = group[group.find("(")+1:group.find(")")].replace("/", "")

        diagram = grt.root.wb.doc.physicalModels[0].diagrams[0]
        grt.modules.Workbench.activateDiagram(diagram)
        tables = set(tables)
        connected_tables = set()
        for connection in diagram.connections:
            # Determine if the connection should be visible
            # Skip largest group table connections outside of largest group
            if connection.foreignKey.owner.name in tables and \
                (connection.foreignKey.referencedTable.name in tables or \
                connection.foreignKey.referencedTable.name not in max_tables):
                connection.visible = 1
                connected_tables.add(connection.foreignKey.referencedTable.name)
                connected_tables.add(connection.foreignKey.owner.name)
            else:
                connection.visible = 0

        for table in diagram.rootLayer.figures:
            # Determine if the table should be visible
            if table.table.name in tables:
                table.visible = 1
            elif table.table.name in connected_tables:
                table.expanded = 0
                table.visible = 1
            else:
                table.visible = 0

        # Export the PDF
        grt.modules.Workbench.exportPDF(str(path / f"group-{name}.pdf"))

    # Open original file
    grt.modules.Workbench.openModel(grt.root.wb.docPath)

if __name__ == "__main__":
    main()
