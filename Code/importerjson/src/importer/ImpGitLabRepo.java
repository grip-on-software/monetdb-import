/**
 * GitLab repository importer.
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

import dao.RepositoryDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Timestamp;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 * Importer for GitLab repositories.
 * @author Leon Helwerda
 */
public class ImpGitLabRepo extends BaseImport {
    
    @Override
    public void parser() {
        int project_id = getProjectID();
        JSONParser parser = new JSONParser();
 
        try (
            RepositoryDb repoDb = new RepositoryDb();
            FileReader fr = new FileReader(getMainImportPath())
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
                JSONObject jsonObject = (JSONObject) o;
                
                String repo_name = (String) jsonObject.get("repo_name");
                String gitlab = (String) jsonObject.get("gitlab_id");
                String description = (String) jsonObject.get("description");
                String create_time = (String) jsonObject.get("create_time");
                String archived = (String) jsonObject.get("archived");
                String avatar = (String) jsonObject.get("has_avatar");
                String stars = (String) jsonObject.get("star_count");
                
                int repo_id = repoDb.check_repo(repo_name, project_id);
                if (repo_id == 0) {
                    repoDb.insert_repo(repo_name, project_id);
                    repo_id = repoDb.check_repo(repo_name, project_id);
                }
                
                if (description.equals("0")) {
                    description = null;
                }
                int gitlab_id = Integer.parseInt(gitlab);
                Timestamp create_date = Timestamp.valueOf(create_time);
                boolean is_archived = archived.equals("1");
                boolean has_avatar = avatar.equals("1");
                int star_count = Integer.parseInt(stars);
                
                RepositoryDb.CheckResult result = repoDb.check_gitlab_repo(repo_id, gitlab_id, description, create_date, is_archived, has_avatar, star_count);
                if (result == RepositoryDb.CheckResult.DIFFERS) {
                    repoDb.update_gitlab_repo(repo_id, gitlab_id, description, create_date, is_archived, has_avatar, star_count);
                }
                else if (result == RepositoryDb.CheckResult.MISSING) {
                    repoDb.insert_gitlab_repo(repo_id, gitlab_id, description, create_date, is_archived, has_avatar, star_count);
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
        return "GitLab repositories";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_gitlab_repo.json"};
    }
    
}
