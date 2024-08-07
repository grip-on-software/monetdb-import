/**
 * Metadata importer.
 * 
 * Copyright 2017-2020 ICTU
 * Copyright 2017-2022 Leiden University
 * Copyright 2017-2024 Leon Helwerda
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package importer;

import dao.TableDb;
import java.io.FileReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 * Importer for metadata tables that only have an id--name relation.
 * @author Leon Helwerda
 */
public class ImportTable extends BaseImport {
    private String name;
    private String fieldName;
    private String metadataName = null;
    private String description;
    
    /**
     * Create an importer for a table with only an id--name relation
     * @param name File name part (excluding 'data_' and '.json' affixes) to import,
     *   as well as the table name in the database, passed along to a TableDb
     * @param fieldName Field name of the name field, both in the JSON file and database
     * @param description Text describing what the table imports
     */
    public ImportTable(String name, String fieldName, String description) {
        this.name = name;
        this.fieldName = fieldName;
        this.description = description;
    }

    /**
     * Create an importer for a table an id--name relation plus additional metadata
     * @param name File name part (excluding 'data_' and '.json' affixes) to import
     *   as well as the table name in the database, passed along to a TableDb
     * @param fieldName Field name of the name field, both in the JSON file and database
     * @param metadataName Field name of the metadata field, both in the JSON file and database
     * @param description Text describing what the table imports
     */
    public ImportTable(String name, String fieldName, String metadataName, String description) {
        this(name, fieldName, description);
        this.metadataName = metadataName;
    }
    
    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        Integer row_id;
         
        try (
            FileReader fr = new FileReader(getMainImportPath());
            TableDb db = new TableDb(name, fieldName, metadataName);
        ) {
            JSONArray jsonArray = (JSONArray) parser.parse(fr);
            
            for (Object object : jsonArray) {
                JSONObject jsonObject = (JSONObject) object;
                
                String id = (String) jsonObject.get("id");
                String value = (String) jsonObject.get(fieldName);
                
                int identifier = Integer.parseInt(id);
                row_id = db.check(identifier);
            
                if (row_id == null) {
                    if (metadataName != null) {
                        String metadata = (String) jsonObject.get(metadataName);
                        db.insert(identifier, value, metadata);
                    }
                    else {
                        db.insert(identifier, value);
                    }
                }
            }
        }
        catch (Exception ex) {
            logException(ex);
        }

    }

    @Override
    public String getImportName() {
        return description;
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_"+name+".json"};
    }
    
}
