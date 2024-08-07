/**
 * JIRA component and issue-component link importer.
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

import java.io.FileReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import dao.ComponentDb;
import java.io.File;
import java.sql.Timestamp;
import util.BaseImport;
import util.BaseLinkDb.CheckResult;

/**
 * Importer for JIRA issue component links and the components themselves.
 * @author Leon Helwerda
 */
public class ImpComponent extends BaseImport {

    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
 
        int project = getProjectID();
        File path = getExportPath();
        try (
            FileReader cmpFile = new FileReader(new File(path, "data_component.json"));
            FileReader linkFile = new FileReader(new File(path, "data_issue_component.json"));
            ComponentDb cmpDb = new ComponentDb()
        ) {
            JSONArray a;
            
            a = (JSONArray) parser.parse(cmpFile);
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                String component_id = (String) jsonObject.get("id");
                String name = (String) jsonObject.get("name");
                String description = (String) jsonObject.get("description");
                
                int component = Integer.parseInt(component_id);
                if ("0".equals(description)) {
                    description = null;
                }
                
                CheckResult.State state = cmpDb.check_component(project, component, name, description);
                if (state == CheckResult.State.MISSING) {
                    cmpDb.insert_component(project, component, name, description);
                }
                else if (state == CheckResult.State.DIFFERS) {
                    cmpDb.update_component(project, component, name, description);
                }
            }
            
            a = (JSONArray) parser.parse(linkFile);
            
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                String issue_id = (String) jsonObject.get("issue_id");
                String component_id = (String) jsonObject.get("component_id");
                String start_date = (String) jsonObject.get("start_date");
                String end_date = (String) jsonObject.get("end_date");
                
                int issue = Integer.parseInt(issue_id);
                int component = Integer.parseInt(component_id);

                Timestamp start;
                if (start_date.equals("0")) {
                    start = null;
                }
                else {
                    start = Timestamp.valueOf(start_date);
                }
                Timestamp end;
                if (end_date.equals("0")) {
                    end = null;
                }
                else {
                    end = Timestamp.valueOf(end_date);
                }
                
                CheckResult result = cmpDb.check_link(issue, component, start, end);
                if (result.state == CheckResult.State.MISSING) {
                    cmpDb.insert_link(issue, component, start, end);
                }
                else if (result.state == CheckResult.State.DIFFERS) {
                    // Ensure we take the earliest start date and latest end date.
                    if (result.dates.start_date != null &&
                            (start == null || start.after(result.dates.start_date))) {
                        start = result.dates.start_date;
                    }
                    if (result.dates.end_date != null && end != null && end.before(result.dates.end_date)) {
                        end = result.dates.end_date;
                    }
                    cmpDb.update_link(issue, component, start, end);
                }
            }
        }
        catch (Exception ex) {
            logException(ex);
        }
    }

    @Override
    public String getImportName() {
        return "JIRA issue components";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_issue_component.json", "data_component.json"};
    }
    
}
