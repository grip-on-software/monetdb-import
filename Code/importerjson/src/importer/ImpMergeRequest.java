/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.DeveloperDb;
import dao.DeveloperDb.Developer;
import dao.MergeRequestDb;
import dao.RepositoryDb;
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
 * Importer for GitLab merge requests or Team Foundation Server pull requests.
 * @author Leon Helwerda
 */
public class ImpMergeRequest extends BaseImport {

    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        int project_id = this.getProjectID();
 
        try (
            RepositoryDb repoDb = new RepositoryDb();
            DeveloperDb devDb = new DeveloperDb();
            SprintDb sprintDb = new SprintDb();
            MergeRequestDb requestDb = new MergeRequestDb();
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
                String source_branch = (String) jsonObject.get("source_branch");
                String target_branch = (String) jsonObject.get("target_branch");
                String author = (String) jsonObject.get("author");
                String author_username = (String) jsonObject.get("author_username");
                String assignee = (String) jsonObject.get("assignee");
                String assignee_username = (String) jsonObject.get("assignee_username");
                String upvotes = (String) jsonObject.get("upvotes");
                String downvotes = (String) jsonObject.get("downvotes");
                String created_at = (String) jsonObject.get("created_at");
                String updated_at = (String) jsonObject.get("updated_at");
                String encrypted = (String) jsonObject.get("encrypted");
                
                int encryption = SaltDb.Encryption.parseInt(encrypted);
                int repo_id = repoDb.check_repo(repo_name, project_id);
                if (repo_id == 0) {
                    throw new Exception("Cannot determine repository: " + repo_name);
                }
                int request_id = Integer.parseInt(id);
                
                int number_of_upvotes = Integer.parseInt(upvotes);
                int number_of_downvotes = Integer.parseInt(downvotes);
                Timestamp created_date = Timestamp.valueOf(created_at);
                Timestamp updated_date = Timestamp.valueOf(updated_at);
                
                Developer author_dev = new Developer(author_username, author, null);
                int author_id = devDb.update_vcs_developer(project_id, author_dev, encryption);
                
                Integer assignee_id;
                if (assignee.equals("0")) {
                    assignee_id = null;
                }
                else {
                    Developer assignee_dev = new Developer(assignee_username, assignee, null);
                    assignee_id = devDb.update_vcs_developer(project_id, assignee_dev, encryption);
                }
                
                MergeRequestDb.CheckResult result = requestDb.check_request(repo_id, request_id, title, description, status, source_branch, target_branch, author_id, assignee_id, number_of_upvotes, number_of_downvotes, created_date, updated_date);
                if (result != MergeRequestDb.CheckResult.EXISTS) {
                    int sprint_id = sprintDb.find_sprint(project_id, created_date);
                    if (result == MergeRequestDb.CheckResult.MISSING) {
                        requestDb.insert_request(repo_id, request_id, title, description, status, source_branch, target_branch, author_id, assignee_id, number_of_upvotes, number_of_downvotes, created_date, updated_date, sprint_id);
                    }
                    else if (result == MergeRequestDb.CheckResult.DIFFERS) {
                        requestDb.update_request(repo_id, request_id, title, description, status, source_branch, target_branch, author_id, assignee_id, number_of_upvotes, number_of_downvotes, created_date, updated_date, sprint_id);
                    }
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
        return "GitLab/TFS merge requests";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_merge_request.json"};
    }
    
}
