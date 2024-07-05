/**
 * Quality metric target version importer.
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

import dao.DeveloperDb;
import dao.DeveloperDb.Developer;
import dao.MetricDb;
import dao.SaltDb;
import dao.SprintDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Timestamp;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 * Importer for metric versions.
 * @author Leon Helwerda
 */
public class ImpMetricVersion extends BaseImport {
    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        int projectId = this.getProjectID();
        String version_id = null;
 
        try (
            MetricDb metricDb = new MetricDb();
            SprintDb sprintDb = new SprintDb();
            DeveloperDb devDb = new DeveloperDb();
            FileReader fr = new FileReader(getMainImportPath())
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                String message = (String) jsonObject.get("message");
                String developer = (String) jsonObject.get("developer");
                String email = (String) jsonObject.get("email");
                String revision = (String) jsonObject.get("version_id");
                String date = (String) jsonObject.get("commit_date");
                
                version_id = metricDb.check_version(projectId, revision);
            
                if (version_id == null) {
                    Timestamp commit_date = Timestamp.valueOf(date);
                    int sprint_id = sprintDb.find_sprint(projectId, commit_date);

                    if (developer != null) {
                        Developer dev = new Developer(developer, email);
                        Integer jira_id = devDb.check_ldap_developer(projectId, dev, SaltDb.Encryption.NONE);
                        // check whether the developer does not already exist
                        if (jira_id == null) {
                            jira_id = devDb.check_developer(dev);
                            devDb.insert_ldap_developer(projectId, jira_id, dev);
                        }
                    }

                    metricDb.insert_version(projectId, revision, developer, message, commit_date, sprint_id);
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
        return "metric versions";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_metric_versions.json"};
    }
}
