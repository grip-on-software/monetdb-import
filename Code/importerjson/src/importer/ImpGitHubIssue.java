/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.DeveloperDb;
import dao.GitHubIssueDb;
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
                    throw new Exception("Cannot determine repository: " + repo_name);
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
                
                DeveloperDb.Developer author_dev = new DeveloperDb.Developer(author, author, null);
                int author_id = devDb.update_vcs_developer(project_id, author_dev, encryption);
                
                Integer assignee_id;
                if (assignee.equals("0")) {
                    assignee_id = null;
                }
                else {
                    DeveloperDb.Developer assignee_dev = new DeveloperDb.Developer(assignee, assignee, null);
                    assignee_id = devDb.update_vcs_developer(project_id, assignee_dev, encryption);
                }

                Integer closer_id;
                if (closed_by.equals("0")) {
                    closer_id = null;
                }
                else {
                    DeveloperDb.Developer closer_dev = new DeveloperDb.Developer(closed_by, closed_by, null);
                    closer_id = devDb.update_vcs_developer(project_id, closer_dev, encryption);
                }
                
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
    
}
