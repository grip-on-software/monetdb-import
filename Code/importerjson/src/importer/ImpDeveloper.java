/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.DataSource;
import dao.DeveloperDb;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
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
        DeveloperDb devDb;
 
        try {
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+projectN+"/data_developer.json"));
            
            for (Object o : a)
            {
                JSONObject jsonObject = (JSONObject) o;
                
                String display_name = (String) jsonObject.get("display_name");
                String name = (String) jsonObject.get("name");
                
                devDb = new DeveloperDb();
                int dev_id = devDb.check_developer(display_name);
                // if already exists
                if(dev_id == 0) {
                	devDb.insert_developer(name, display_name);
                }

            }
                  
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
        

}
    
