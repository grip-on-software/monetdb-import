/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import util.BaseImport;
import dao.StatusDb;
import java.io.BufferedReader;
import java.io.FileReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Enrique
 */
public class ImpDataStatus extends BaseImport{
    
    public void parser(String projectN){

        BufferedReader br = null;
        JSONParser parser = new JSONParser();
        StatusDb statusDB;
        int stat_id = 0;
 
        try {
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+projectN+"/data_status.json"));
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String description = (String) jsonObject.get("description");
                String id = (String) jsonObject.get("id");
                String name = (String) jsonObject.get("name");
                
                statusDB = new StatusDb();
                stat_id = statusDB.check_status(name);
            
                if(stat_id == 0){

                    statusDB.insert_status(Integer.parseInt(id),name,description);
                    
                }
            }
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
        

}
    
