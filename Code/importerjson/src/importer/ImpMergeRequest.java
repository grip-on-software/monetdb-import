/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.MergeRequestDb;
import dao.RepositoryDb;
import dao.SaltDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Timestamp;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 *
 * @author leonhelwerda
 */
public class ImpMergeRequest extends BaseImport {

    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
 
        try (
            RepositoryDb repoDb = new RepositoryDb();
            MergeRequestDb requestDb = new MergeRequestDb();
            FileReader fr = new FileReader(getPath()+getProjectName()+"/data_merge_request.json")
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
                JSONObject jsonObject = (JSONObject) o;
                
                String repo_name = (String) jsonObject.get("repo_name");
                String id = (String) jsonObject.get("id");
                String title = (String) jsonObject.get("title");
                String description = (String) jsonObject.get("description");
                String source_branch = (String) jsonObject.get("source_branch");
                String target_branch = (String) jsonObject.get("target_branch");
                String author = (String) jsonObject.get("author");
                String assignee = (String) jsonObject.get("assignee");
                String upvotes = (String) jsonObject.get("upvotes");
                String downvotes = (String) jsonObject.get("downvotes");
                String created_at = (String) jsonObject.get("created_at");
                String updated_at = (String) jsonObject.get("updated_at");
                String encrypted = (String) jsonObject.get("encrypted");
                
                int repo_id = repoDb.check_repo(repo_name);
                if (repo_id == 0) {
                    throw new Exception("Cannot determine repository: " + repo_name);
                }
                int request_id = Integer.parseInt(id);
                
                int number_of_upvotes = Integer.parseInt(upvotes);
                int number_of_downvotes = Integer.parseInt(downvotes);
                Timestamp created_date = Timestamp.valueOf(created_at);
                Timestamp updated_date = Timestamp.valueOf(updated_at);
                if (assignee.equals("0")) {
                    assignee = null;
                }
                int encryption = SaltDb.Encryption.parseInt(encrypted);
                
                MergeRequestDb.CheckResult result = requestDb.check_request(repo_id, request_id, title, description, source_branch, target_branch, author, assignee, number_of_upvotes, number_of_downvotes, created_date, updated_date, encryption);
                if (result == MergeRequestDb.CheckResult.MISSING) {
                    requestDb.insert_request(repo_id, request_id, title, description, source_branch, target_branch, author, assignee, number_of_upvotes, number_of_downvotes, created_date, updated_date, encryption);
                }
                else if (result == MergeRequestDb.CheckResult.DIFFERS) {
                    requestDb.update_request(repo_id, request_id, title, description, source_branch, target_branch, author, assignee, number_of_upvotes, number_of_downvotes, created_date, updated_date, encryption);
                }
            }
            
        }
        catch (FileNotFoundException ex) {
            System.out.println("Cannot import " + getImportName() + ": " + ex.getMessage());
        }
        catch (Exception ex) {
            logException(ex);
        }

    }

    @Override
    public String getImportName() {
        return "GitLab merge requests";
    }
    
}
