/**
 * JIRA status category importer.
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
 * Importer for JIRA status categories.
 * @author Leon Helwerda
 */
public class ImpJiraStatusCategory extends BaseImport {

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
                
                Long id = (Long) jsonObject.get("id");
                String key = (String) jsonObject.get("key");
                String name = (String) jsonObject.get("name");
                String color = (String) jsonObject.get("color");
                
                int category_id = id.intValue();
                
                StatusDb.CheckResult result = statusDb.check_category(category_id, key, name, color);
                if (result == StatusDb.CheckResult.MISSING) {
                    statusDb.insert_category(category_id, key, name, color);
                }
                else if (result == StatusDb.CheckResult.DIFFERS) {
                    statusDb.update_category(category_id, key, name, color);
                }
            }
        }
        catch (Exception ex) {
            logException(ex);
        }        
    }

    @Override
    public String getImportName() {
        return "JIRA status categories";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_status_category.json"};
    }
    
}
