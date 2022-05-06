/**
 * TFS developer importer.
 * 
 * Copyright 2017-2020 ICTU
 * Copyright 2017-2022 Leiden University
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

import dao.DeveloperDb;
import dao.SaltDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 * Importer for TFS developers extracted from work items.
 * @author Thomas, Enrique, Leon Helwerda
 */
public class ImpTfsDeveloper extends BaseImport {
    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        int project_id = this.getProjectID();
 
        try (
            DeveloperDb devDb = new DeveloperDb();
            FileReader fr = new FileReader(getMainImportPath())
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                String display_name = (String) jsonObject.get("display_name");
                String email = (String) jsonObject.get("email");
                String encrypted = (String) jsonObject.get("encrypted");
                int encryption = SaltDb.Encryption.parseInt(encrypted);
                
                DeveloperDb.Developer dev = new DeveloperDb.Developer(null, display_name, email);
                Integer alias_id = devDb.check_tfs_developer(project_id, dev, encryption);
                // check whether the developer does not already exist
                if (alias_id == null) {
                    alias_id = devDb.update_vcs_developer(project_id, dev, encryption);
                    devDb.insert_tfs_developer(project_id, alias_id, dev, encryption);
                }
            }                  
        }
        catch (FileNotFoundException ex) {
            getLogger().log(Level.WARNING, "Cannot import {0}: {1}", new Object[]{getImportName(), ex.getMessage()});
        }
        catch (Exception ex) {
            logException(ex);
        }
        
    }

    @Override
    public String getImportName() {
        return "TFS work item developers";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_tfs_developer.json"};
    }
    
}
