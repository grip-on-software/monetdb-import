/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.DeveloperDb;
import java.io.BufferedReader;
import java.io.FileReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 *
 * @author Thomas and Enrique
 */
public class ImpDeveloper extends BaseImport{
    
    public void parser(String projectN){

        BufferedReader br = null;
        JSONParser parser = new JSONParser();
        DeveloperDb devDb = new DeveloperDb();
 
        try {
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+projectN+"/data_developer.json"));
            
            for (Object o : a)
            {
                JSONObject jsonObject = (JSONObject) o;
                
                String display_name = (String) jsonObject.get("display_name");
                String name = (String) jsonObject.get("name");
                name = addSlashes(name);
                display_name = addSlashes(display_name);
                
                int dev_id = devDb.check_developer(display_name);
                // check whether the developer does not already exist
                if(dev_id == 0) {
                    devDb.insert_developer(name, display_name);
                }

            }
            
            devDb.close();
                  
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    public static String addSlashes(String s) {
        s = s.replaceAll("\\\\", "\\\\\\\\");
        s = s.replaceAll("\\n", "\\\\n");
        s = s.replaceAll("\\r", "\\\\r");
        s = s.replaceAll("\\00", "\\\\0");
        s = s.replaceAll("'", "\\\\'");
    return s;
}
        

}
    
