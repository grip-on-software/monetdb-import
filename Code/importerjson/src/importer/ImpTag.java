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
import dao.SprintDb;
import dao.TagDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Timestamp;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 * Importer for VCS tags.
 * @author Leon Helwerda
 */
public class ImpTag extends BaseImport {

    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        int projectID = getProjectID();

        try (
            DeveloperDb devDb = new DeveloperDb();
            RepositoryDb repoDb = new RepositoryDb();
            SprintDb sprintDb = new SprintDb();
            TagDb tagDb = new TagDb();
            FileReader fr = new FileReader(getMainImportPath())
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                String repo_name = (String) jsonObject.get("repo_name");
                String tag_name = (String) jsonObject.get("tag_name");
                String version_id = (String) jsonObject.get("version_id");
                String message = (String) jsonObject.get("message");
                String tagged_date = (String) jsonObject.get("tagged_date");
                String tagger = (String) jsonObject.get("tagger");
                String tagger_email = (String) jsonObject.get("tagger_email");
                String encrypted = (String) jsonObject.get("encrypted");
                
                int repo_id = repoDb.check_repo(repo_name, projectID);
                if (repo_id == 0){
                    repoDb.insert_repo(repo_name, projectID);
                    repo_id = repoDb.check_repo(repo_name, projectID);
                }
                
                int encryption = SaltDb.Encryption.parseInt(encrypted);
                
                if (message.equals("0")) {
                    message = null;
                }
                Timestamp tag_date;
                if (tagged_date.equals("0")) {
                    tag_date = null;
                }
                else {
                    tag_date = Timestamp.valueOf(tagged_date);
                }
                
                Integer tagger_id;
                if (tagger_email.equals("0")) {
                    tagger_email = null;
                }
                
                if (tagger.equals("0")) {
                    tagger_id = null;
                }
                else {
                    Developer dev = new Developer(tagger, tagger_email);
                    tagger_id = devDb.update_vcs_developer(projectID, dev, encryption);
                }
                
                TagDb.CheckResult result = tagDb.check_tag(repo_id, tag_name, version_id, message, tag_date, tagger_id);
                if (result != TagDb.CheckResult.EXISTS) {
                    int sprint_id = sprintDb.find_sprint(projectID, tag_date);
                    if (result == TagDb.CheckResult.DIFFERS) {
                        tagDb.update_tag(repo_id, tag_name, version_id, message, tag_date, tagger_id, sprint_id);
                    }
                    else if (result == TagDb.CheckResult.MISSING) {
                        tagDb.insert_tag(repo_id, tag_name, version_id, message, tag_date, tagger_id, sprint_id);
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
        return "version control system tags";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_tag.json"};
    }
    
}
