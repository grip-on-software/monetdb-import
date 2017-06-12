/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.DeveloperDb;
import dao.DeveloperDb.Developer;
import java.io.FileReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 * Importer for JIRA developers.
 * @author Thomas and Enrique
 */
public class ImpDeveloper extends BaseImport{
    
    @Override
    public void parser(){
        JSONParser parser = new JSONParser();
        int project_id = this.getProjectID();
 
        try (
            DeveloperDb devDb = new DeveloperDb();
            FileReader fr = new FileReader(getPath()+getProjectName()+"/data_developer.json")
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                String display_name = (String) jsonObject.get("display_name");
                String name = (String) jsonObject.get("name");
                String email = (String) jsonObject.get("email");
                
                Developer dev = new Developer(name, display_name, email);
                int dev_id = devDb.check_developer(dev);
                // check whether the developer does not already exist
                if(dev_id == 0) {
                    devDb.insert_developer(dev);
                    dev_id = devDb.check_developer(dev);
                }
                devDb.insert_project_developer(project_id, dev_id, dev);

            }                  
        }
        catch (Exception ex) {
            logException(ex);
        }
        
    }

    @Override
    public String getImportName() {
        return "JIRA developers";
    }

}
    
