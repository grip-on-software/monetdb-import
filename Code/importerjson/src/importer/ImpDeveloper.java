/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.DeveloperDb;
import java.io.FileReader;
import java.sql.SQLException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 *
 * @author Thomas and Enrique
 */
public class ImpDeveloper extends BaseImport{
    
    @Override
    public void parser(){

        JSONParser parser = new JSONParser();
        DeveloperDb devDb = new DeveloperDb();
 
        try {
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+getProjectName()+"/data_developer.json"));
            
            for (Object o : a)
            {
                JSONObject jsonObject = (JSONObject) o;
                
                String display_name = (String) jsonObject.get("display_name");
                String name = (String) jsonObject.get("name");
                
                int dev_id = devDb.check_developer(name, display_name);
                // check whether the developer does not already exist
                if(dev_id == 0) {
                    devDb.insert_developer(name, display_name);
                }

            }
            
            devDb.close();
                  
        }
            
        catch (SQLException e) {
            printSQLExceptionDetails(e);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }

}
    
