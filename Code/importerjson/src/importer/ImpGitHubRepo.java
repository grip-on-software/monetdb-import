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
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 * Importer for GitHub repositories.
 * @author Leon Helwerda
 */
public class ImpGitHubRepo extends BaseImport {

    @Override
    public void parser() {
        int project_id = getProjectID();
        JSONParser parser = new JSONParser();
 
        try (
            RepositoryDb repoDb = new RepositoryDb();
            FileReader fr = new FileReader(getPath()+getProjectName()+"/data_github_repo.json")
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
                JSONObject jsonObject = (JSONObject) o;
                
                String repo_name = (String) jsonObject.get("repo_name");
                String github = (String) jsonObject.get("github_id");
                String description = (String) jsonObject.get("description");
                String create_time = (String) jsonObject.get("create_time");
                String private_repo = (String) jsonObject.get("private");
                String forked = (String) jsonObject.get("forked");
                String stars = (String) jsonObject.get("star_count");
                String watchers = (String) jsonObject.get("watch_count");
                
                int repo_id = repoDb.check_repo(repo_name, project_id);
                if (repo_id == 0) {
                    repoDb.insert_repo(repo_name, project_id);
                    repo_id = repoDb.check_repo(repo_name, project_id);
                }
                
                if (description.equals("0")) {
                    description = null;
                }
                int github_id = Integer.parseInt(github);
                Timestamp create_date = Timestamp.valueOf(create_time);
                boolean is_private = private_repo.equals("1");
                boolean is_forked = forked.equals("1");
                int star_count = Integer.parseInt(stars);
                int watch_count = Integer.parseInt(watchers);
                
                RepositoryDb.CheckResult result = repoDb.check_github_repo(repo_id, github_id, description, create_date, is_private, is_forked, star_count, watch_count);
                if (result == RepositoryDb.CheckResult.DIFFERS) {
                    repoDb.update_github_repo(repo_id, github_id, description, create_date, is_private, is_forked, star_count, watch_count);
                }
                else if (result == RepositoryDb.CheckResult.MISSING) {
                    repoDb.insert_github_repo(repo_id, github_id, description, create_date, is_private, is_forked, star_count, watch_count);
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
        return "GitHub repositories";
    }
    
}
