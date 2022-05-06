/**
 * Jenkins status importer.
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

import dao.JenkinsDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Level;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 * Importer for Jenkins usage statistics.
 * @author Leon Helwerda
 */
public class ImpJenkins extends BaseImport {

    @Override
    public void parser() {
        int projectID = getProjectID();
        JSONParser parser = new JSONParser();
        try (
            JenkinsDb jenkinsDb = new JenkinsDb();
            FileReader fr = new FileReader(getMainImportPath());
        ) {
            JSONObject jsonObject = (JSONObject) parser.parse(fr);
            String host = (String) jsonObject.get("host");
            Long jobs = (Long) jsonObject.get("jobs");
            Long views = (Long) jsonObject.get("views");
            Long nodes = (Long) jsonObject.get("nodes");
            
            int num_jobs = jobs.intValue();
            int num_views = views.intValue();
            int num_nodes = nodes.intValue();
            
            JenkinsDb.CheckResult result = jenkinsDb.check(projectID, host, num_jobs, num_views, num_nodes);
            if (result == JenkinsDb.CheckResult.MISSING) {
                jenkinsDb.insert(projectID, host, num_jobs, num_views, num_nodes);
            }
            else if (result == JenkinsDb.CheckResult.DIFFERS) {
                jenkinsDb.update(projectID, host, num_jobs, num_views, num_nodes);
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
        return "Jenkins usage statistics";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_jenkins.json"};
    }
    
}
