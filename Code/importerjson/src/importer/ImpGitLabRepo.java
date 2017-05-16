/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.RepositoryDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Timestamp;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 * Importer for GitLab repositories.
 * @author Leon Helwerda
 */
public class ImpGitLabRepo extends BaseImport{
    
    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
 
        try (
            RepositoryDb repoDb = new RepositoryDb();
            FileReader fr = new FileReader(getPath()+getProjectName()+"/data_gitlab_repo.json")
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
                
                int repo_id = repoDb.check_repo(repo_name);
                if (repo_id == 0) {
                    repoDb.insert_repo(repo_name);
                    repo_id = repoDb.check_repo(repo_name);
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
            System.out.println("Cannot import " + getImportName() + ": " + ex.getMessage());
        }
        catch (Exception ex) {
            logException(ex);
        }

    }

    @Override
    public String getImportName() {
        return "GitLab repositories";
    }
    
}
