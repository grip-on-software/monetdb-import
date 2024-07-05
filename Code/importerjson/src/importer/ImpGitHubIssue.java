/**
 * GitHub issue importer.
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
import dao.GitHubIssueDb;
import dao.RepositoryDb;
import dao.SaltDb;
import java.beans.PropertyVetoException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 * Importer for GitHub issues.
 * @author Leon Helwerda
 */
public class ImpGitHubIssue extends BaseImport {

    @Override
    public void parser() {
        int project_id = getProjectID();
        JSONParser parser = new JSONParser();
        
        try (
            RepositoryDb repoDb = new RepositoryDb();
            DeveloperDb devDb = new DeveloperDb();
            GitHubIssueDb issueDb = new GitHubIssueDb();
            FileReader fr = new FileReader(getMainImportPath())
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
                JSONObject jsonObject = (JSONObject) o;
                
                String repo_name = (String) jsonObject.get("repo_name");
                String id = (String) jsonObject.get("id");
                String title = (String) jsonObject.get("title");
                String description = (String) jsonObject.get("description");
                String status = (String) jsonObject.get("status");
                String author = (String) jsonObject.get("author");
                String assignee = (String) jsonObject.get("assignee");
                String request_id = (String) jsonObject.get("pull_request_id");
                String labels = (String) jsonObject.get("labels");
                String closed_by = (String) jsonObject.get("closed_by");
                String closed_at = (String) jsonObject.get("closed_at");
                String created_at = (String) jsonObject.get("created_at");
                String updated_at = (String) jsonObject.get("updated_at");
                String encrypted = (String) jsonObject.get("encrypted");
                
                int encryption = SaltDb.Encryption.parseInt(encrypted);
                int repo_id = repoDb.check_repo(repo_name, project_id);
                if (repo_id == 0) {
                    getLogger().log(Level.WARNING, "Cannot determine repository in {0}: {1}", new Object[]{getMainImportPath(), repo_name});
                    continue;
                }
                int issue_id = Integer.parseInt(id);
                Integer pull_request_id = Integer.parseInt(request_id);
                if (pull_request_id == 0) {
                    pull_request_id = null;
                }
                
                int number_of_labels = Integer.parseInt(labels);

                Timestamp created_date = Timestamp.valueOf(created_at);
                Timestamp updated_date = Timestamp.valueOf(updated_at);
                Timestamp closed_date = null;
                if (!closed_at.equals("0")) {
                    closed_date = Timestamp.valueOf(closed_at);
                }
                
                Integer developer = parseDeveloper(author, devDb, project_id, encryption);
                if (developer == null) {
                    throw new ImporterException("Author ID required");
                }
                int author_id = developer;
                
                Integer assignee_id = parseDeveloper(assignee, devDb, project_id, encryption);
                Integer closer_id = parseDeveloper(closed_by, devDb, project_id, encryption);
                
                GitHubIssueDb.CheckResult result = issueDb.check_issue(repo_id, issue_id, title, description, status, author_id, assignee_id, created_date, updated_date, pull_request_id, number_of_labels, closed_date, closer_id);
                if (result == GitHubIssueDb.CheckResult.MISSING) {
                    issueDb.insert_issue(repo_id, issue_id, title, description, status, author_id, assignee_id, created_date, updated_date, pull_request_id, number_of_labels, closed_date, closer_id);
                }
                else if (result == GitHubIssueDb.CheckResult.DIFFERS) {
                    issueDb.update_issue(repo_id, issue_id, title, description, status, author_id, assignee_id, created_date, updated_date, pull_request_id, number_of_labels, closed_date, closer_id);
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
        return "GitHub issues";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_github_issue.json"};
    }

    private Integer parseDeveloper(String developer, DeveloperDb devDb, int project_id, int encryption) throws SQLException, PropertyVetoException {
        if (developer.equals("0")) {
            return null;
        }
        else {
            DeveloperDb.Developer assignee_dev = new DeveloperDb.Developer(developer, developer, null);
            return devDb.update_vcs_developer(project_id, assignee_dev, encryption);
        }
    }
    
}
