/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.DeveloperDb;
import dao.RepositoryDb;
import dao.TagDb;
import java.io.FileReader;
import java.sql.Timestamp;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 *
 * @author Leon Helwerda
 */
public class ImpTag extends BaseImport {

    @Override
    public void parser() {
        JSONParser parser = new JSONParser();

        try (
                DeveloperDb devDb = new DeveloperDb();
                RepositoryDb repoDb = new RepositoryDb();
                TagDb tagDb = new TagDb();
                FileReader fr = new FileReader(getPath()+getProjectName()+"/data_tag.json")
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
                JSONObject jsonObject = (JSONObject) o;
                
                String repo_name = (String) jsonObject.get("repo_name");
                String tag_name = (String) jsonObject.get("tag_name");
                String version_id = (String) jsonObject.get("version_id");
                String message = (String) jsonObject.get("message");
                String tagged_date = (String) jsonObject.get("tagged_date");
                String tagger = (String) jsonObject.get("tagger");
                
                int repo_id = repoDb.check_repo(repo_name);
                if (repo_id == 0){
                    throw new Exception("Cannot determine repository: " + repo_name);
                }
                
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
                if (tagger.equals("0")) {
                    tagger_id = null;
                }
                else {
                    tagger_id = devDb.check_vcs_developer(tagger);
                    if (tagger_id == 0) {
                        int dev_id = devDb.check_developer(tagger, tagger);
                        devDb.insert_vcs_developer(dev_id, tagger);
                        tagger_id = devDb.check_vcs_developer(tagger);
                    }
                }
                
                TagDb.CheckResult result = tagDb.check_tag(repo_id, tag_name, version_id, message, tag_date, tagger_id);
                if (result == TagDb.CheckResult.DIFFERS) {
                    tagDb.update_tag(repo_id, tag_name, version_id, message, tag_date, tagger_id);
                }
                else if (result == TagDb.CheckResult.MISSING) {
                    tagDb.insert_tag(repo_id, tag_name, version_id, message, tag_date, tagger_id);
                }

            }
        }
        catch (Exception ex) {
            logException(ex);
        }
    }

    @Override
    public String getImportName() {
        return "version control system tags";
    }
    
}
