/**
 * GitHub issue note importer.
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
import dao.GitHubIssueNoteDb;
import dao.RepositoryDb;
import dao.SaltDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Timestamp;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 * Importer for GitHub issue comments.
 * @author Leon Helwerda
 */
public class ImpGitHubIssueNote extends BaseImport {

    @Override
    public void parser() {
        int project_id = getProjectID();
        JSONParser parser = new JSONParser();
        
        try (
            RepositoryDb repoDb = new RepositoryDb();
            DeveloperDb devDb = new DeveloperDb();
            GitHubIssueNoteDb noteDb = new GitHubIssueNoteDb();
            FileReader fr = new FileReader(getMainImportPath())
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
                JSONObject jsonObject = (JSONObject) o;
                
                String repo_name = (String) jsonObject.get("repo_name");
                String id = (String) jsonObject.get("note_id");
                String issue = (String) jsonObject.get("issue_id");
                String comment = (String) jsonObject.get("comment");
                String author = (String) jsonObject.get("author");
                String created_at = (String) jsonObject.get("created_at");
                String updated_at = (String) jsonObject.get("updated_at");
                String encrypted = (String) jsonObject.get("encrypted");

                int encryption = SaltDb.Encryption.parseInt(encrypted);
                int repo_id = repoDb.check_repo(repo_name, project_id);
                if (repo_id == 0) {
                    getLogger().log(Level.WARNING, "Cannot determine repository in {0}: {1}", new Object[]{getMainImportPath(), repo_name});
                    continue;
                }
                int note_id = Integer.parseInt(id);
                int issue_id = Integer.parseInt(issue);
                
                Timestamp created_date = Timestamp.valueOf(created_at);
                Timestamp updated_date = Timestamp.valueOf(updated_at);

                DeveloperDb.Developer author_dev = new DeveloperDb.Developer(author, author, null);
                int author_id = devDb.update_vcs_developer(project_id, author_dev, encryption);

                GitHubIssueNoteDb.CheckResult result = noteDb.check_note(repo_id, issue_id, note_id, author_id, comment, created_date, updated_date);
                if (result == GitHubIssueNoteDb.CheckResult.MISSING) {
                    noteDb.insert_note(repo_id, issue_id, note_id, author_id, comment, created_date, updated_date);
                }
                else if (result == GitHubIssueNoteDb.CheckResult.DIFFERS) {
                    noteDb.update_note(repo_id, issue_id, note_id, author_id, comment, created_date, updated_date);
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
        return "GitHub issue comments";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_github_issue_note.json"};
    }
    
}
