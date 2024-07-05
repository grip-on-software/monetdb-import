/**
 * JIRA status importer.
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

import dao.StatusDb;
import java.io.FileReader;
import org.json.simple.JSONObject;
import util.BaseImport;
import util.BufferedJSONReader;

/**
 * Importer for JIRA status type metadata.
 * @author Leon Helwerda
 */
public class ImpJiraStatus extends BaseImport {

    @Override
    public void parser() {
        try (
            StatusDb statusDb = new StatusDb();
            FileReader fr = new FileReader(getMainImportPath());
            BufferedJSONReader br = new BufferedJSONReader(fr)
        ) {
            Object o;
            while ((o = br.readObject()) != null) {
                JSONObject jsonObject = (JSONObject) o;
                
                String id = (String) jsonObject.get("id");
                String name = (String) jsonObject.get("name");
                String description = (String) jsonObject.get("description");
                Long status_category = (Long) jsonObject.get("statusCategory");
                
                int status_id = Integer.parseInt(id);
                Integer category_id = status_category == null ? null : status_category.intValue();
                
                StatusDb.CheckResult result = statusDb.check_status(status_id, name, description, category_id);
                if (result == StatusDb.CheckResult.MISSING) {
                    statusDb.insert_status(status_id, name, description, category_id);
                }
                else if (result == StatusDb.CheckResult.DIFFERS) {
                    statusDb.update_status(status_id, name, description, category_id);
                }
            }
        }
        catch (Exception ex) {
            logException(ex);
        }        
    }

    @Override
    public String getImportName() {
        return "JIRA status types";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_status.json"};
    }
    
}
