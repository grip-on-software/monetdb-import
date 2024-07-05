/**
 * JIRA fix version importer.
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

import dao.FixVersionDb;
import util.BaseImport;
import java.io.FileReader;
import java.sql.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Importer for JIRA fix versions.
 * @author Enrique
 */
public class ImpDataFixVersion extends BaseImport {
    
    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        int project = getProjectID();
         
        try (
            FileReader fr = new FileReader(getMainImportPath());
            FixVersionDb versionDb = new FixVersionDb()
        ) {            
            JSONArray a = (JSONArray) parser.parse(fr);
            Set<String> identifiers = new HashSet<>();
            
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                String id = (String) jsonObject.get("id");
                if (identifiers.contains(id)) {
                    getLogger().log(Level.WARNING, "Duplicate identifier in {0}: {1}", new Object[]{getMainImportPath(), id});
                    continue;
                }
                identifiers.add(id);

                String name = (String) jsonObject.get("name");
                String description = (String) jsonObject.get("description");
                String start_date = (String) jsonObject.get("start_date");
                String release_date = (String) jsonObject.get("release_date");
                String released = (String) jsonObject.get("released");
                
                int identifier = Integer.parseInt(id);
                boolean is_released = released.equals("1");
                Date start;
                if (start_date.equals("0")) {
                    start = null;
                }
                else {
                    start = Date.valueOf(start_date);
                }
                Date release;
                if (release_date.equals("0")) {
                    release = null;
                }
                else {
                    release = Date.valueOf(release_date);
                }
                
                FixVersionDb.CheckResult result = versionDb.check_version(identifier, project, name, description, start, release, is_released);
                if (result == FixVersionDb.CheckResult.MISSING) {
                    versionDb.insert_version(identifier, project, name, description, start, release, is_released);
                }
                else if (result == FixVersionDb.CheckResult.DIFFERS) {
                    versionDb.update_version(identifier, project, name, description, start, release, is_released);
                }
            }
        }            
        catch (Exception ex) {
            logException(ex);
        }
    }

    @Override
    public String getImportName() {
        return "JIRA fix and release versions";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_fixVersion.json"};
    }

}
    
