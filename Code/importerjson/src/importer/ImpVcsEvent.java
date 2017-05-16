/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
            FileReader fr = new FileReader(getPath()+getProjectName()+"/data_vcs_event.json")
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                String repo_name = (String) jsonObject.get("repo_name");
                String action = (String) jsonObject.get("action");
                String kind = (String) jsonObject.get("kind");
                String commit_id = (String) jsonObject.get("commit_id");
                String ref = (String) jsonObject.get("ref");
                String date = (String) jsonObject.get("date");
                String user = (String) jsonObject.get("user");
                String email = (String) jsonObject.get("email");
                String encrypted = (String) jsonObject.get("encrypted");
                
                int encryption = SaltDb.Encryption.parseInt(encrypted);
                int repo_id = repoDb.check_repo(repo_name);
                if (repo_id == 0) {
                    repoDb.insert_repo(repo_name);
                    repo_id = repoDb.check_repo(repo_name);
                }

                Timestamp event_date = Timestamp.valueOf(date);
                Developer author_dev = new Developer(user, user, email);
                int author_id = devDb.update_vcs_developer(project_id, author_dev, encryption);
                
                if (!eventDb.check_event(repo_id, action, kind, commit_id, ref, event_date, author_id)) {
                    eventDb.insert_event(repo_id, action, kind, commit_id, ref, event_date, author_id);
                }
            }
        } catch (FileNotFoundException ex) {
            System.out.println("Cannot import " + getImportName() + ": " + ex.getMessage());
        } catch (Exception ex) {
            logException(ex);
        }
    }

    @Override
    public String getImportName() {
        return "version control system events";
    }
    
}
