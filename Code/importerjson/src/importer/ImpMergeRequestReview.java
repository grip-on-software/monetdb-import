/**
 * TFS merge request review importer.
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
import dao.ReviewDb;
import dao.RepositoryDb;
import dao.SaltDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 * Importer for Team Foundation Server pull request reviews.
 * @author Leon Helwerda
 */
public class ImpMergeRequestReview extends BaseImport {

    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        int project_id = this.getProjectID();
 
        try (
            RepositoryDb repoDb = new RepositoryDb();
            DeveloperDb devDb = new DeveloperDb();
            ReviewDb reviewDb = new ReviewDb();
            FileReader fr = new FileReader(getMainImportPath())
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
                JSONObject jsonObject = (JSONObject) o;
                
                String repo_name = (String) jsonObject.get("repo_name");
                String merge_request_id = (String) jsonObject.get("merge_request_id");
                String reviewer = (String) jsonObject.get("reviewer");
                String reviewer_username = (String) jsonObject.get("reviewer_username");
                String weight = (String) jsonObject.get("vote");
                String encrypted = (String) jsonObject.get("encrypted");
                
                int encryption = SaltDb.Encryption.parseInt(encrypted);                
                int repo_id = repoDb.check_repo(repo_name, project_id);
                if (repo_id == 0) {
                    getLogger().log(Level.WARNING, "Cannot determine repository in {0}: {1}", new Object[]{getMainImportPath(), repo_name});
                    continue;
                }
                int request_id = Integer.parseInt(merge_request_id);
                Developer dev = new Developer(reviewer_username, reviewer, reviewer);
                int dev_id = devDb.update_vcs_developer(project_id, dev, encryption);
                int vote = Integer.parseInt(weight);
                
                Integer current_vote = reviewDb.check_review(repo_id, request_id, dev_id);
                if (current_vote == null) {
                    reviewDb.insert_review(repo_id, request_id, dev_id, vote);
                }
                else if (current_vote != vote) {
                    reviewDb.update_review(repo_id, request_id, dev_id, vote);
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
        return "TFS pull request reviews";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_merge_request_review.json"};
    }
    
}
