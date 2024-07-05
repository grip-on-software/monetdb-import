/**
 * VCS review system event importer.
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
import dao.RepositoryDb;
import dao.SaltDb;
import dao.VcsEventDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Timestamp;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 * Importer for VCS events.
 * @author Leon Helwerda
 */
public class ImpVcsEvent extends BaseImport {

    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        int project_id = this.getProjectID();
 
        try (
            RepositoryDb repoDb = new RepositoryDb();
            DeveloperDb devDb = new DeveloperDb();
            VcsEventDb eventDb = new VcsEventDb();
            FileReader fr = new FileReader(getMainImportPath())
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                String repo_name = (String) jsonObject.get("repo_name");
                String action = (String) jsonObject.get("action");
                String kind = (String) jsonObject.get("kind");
                String commit_id = (String) jsonObject.get("version_id");
                String ref = (String) jsonObject.get("ref");
                String date = (String) jsonObject.get("date");
                String user = (String) jsonObject.get("user");
                String username = (String) jsonObject.get("username");
                String email = (String) jsonObject.get("email");
                String encrypted = (String) jsonObject.get("encrypted");
                
                int encryption = SaltDb.Encryption.parseInt(encrypted);
                int repo_id = repoDb.check_repo(repo_name, project_id);
                if (repo_id == 0) {
                    repoDb.insert_repo(repo_name, project_id);
                    repo_id = repoDb.check_repo(repo_name, project_id);
                }

                Timestamp event_date = Timestamp.valueOf(date);
                Developer author_dev = new Developer(username, user, email);
                int author_id = devDb.update_vcs_developer(project_id, author_dev, encryption);
                
                if (!eventDb.check_event(repo_id, action, kind, commit_id, ref, event_date, author_id)) {
                    eventDb.insert_event(repo_id, action, kind, commit_id, ref, event_date, author_id);
                }
            }
        } catch (FileNotFoundException ex) {
            getLogger().log(Level.WARNING, "Cannot import {0}: {1}", new Object[]{getImportName(), ex.getMessage()});
        } catch (Exception ex) {
            logException(ex);
        }
    }

    @Override
    public String getImportName() {
        return "version control system events";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_vcs_event.json"};
    }
    
}
