/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.ReadyStatusDb;
import util.BaseImport;
import java.io.FileReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Enrique and Leon Helwerda
 */
public class ImpReadyStatus extends BaseImport{
    
    public void parser(String projectN){

        JSONParser parser = new JSONParser();
        ReadyStatusDb readyStatusDB = new ReadyStatusDb();
        int stat_id = 0;
 
        try {
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+projectN+"/data_ready_status.json"));
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String id = (String) jsonObject.get("id");
                String name = (String) jsonObject.get("name");
                
                stat_id = readyStatusDB.check_status(name);
            
                if(stat_id == 0){

                    readyStatusDB.insert_status(Integer.parseInt(id),name);
                    
                }
            }
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
        

}
    
