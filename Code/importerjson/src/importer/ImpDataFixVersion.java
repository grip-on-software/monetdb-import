/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.FixVersionDb;
import util.BaseImport;
import java.io.FileReader;
import java.sql.Date;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Importer for JIRA fix versions.
 * @author Enrique
 */
public class ImpDataFixVersion extends BaseImport {
    
    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        int project = getProjectID();
         
        try (
            FileReader fr = new FileReader(getPath()+getProjectName()+"/data_fixVersion.json");
            FixVersionDb versionDb = new FixVersionDb()
        ) {            
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                String id = (String) jsonObject.get("id");
                String name = (String) jsonObject.get("name");
                String description = (String) jsonObject.get("description");
                String start_date = (String) jsonObject.get("start_date");
                String release_date = (String) jsonObject.get("release_date");
                String released = (String) jsonObject.get("released");
                
                int identifier = Integer.parseInt(id);
                boolean is_released = released.equals("1");
                Date start;
                if (start_date.equals("0")) {
                    start = null;
                }
                else {
                    start = Date.valueOf(start_date);
                }
                Date release;
                if (release_date.equals("0")) {
                    release = null;
                }
                else {
                    release = Date.valueOf(release_date);
                }
                
                FixVersionDb.CheckResult result = versionDb.check_version(identifier, project, name, description, start, release, is_released);
                if (result == FixVersionDb.CheckResult.MISSING) {
                    versionDb.insert_version(identifier, project, name, description, start, release, is_released);
                }
                else if (result == FixVersionDb.CheckResult.DIFFERS) {
                    versionDb.update_version(identifier, project, name, description, start, release, is_released);
                }
            }
        }            
        catch (Exception ex) {
            logException(ex);
        }
    }

    @Override
    public String getImportName() {
        return "JIRA fix and release versions";
    }

}
    
