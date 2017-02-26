/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import util.BaseImport;
import dao.PriorityDb;
import java.io.FileReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Enrique
 */
public class ImpDataPriority extends BaseImport{
    
    @Override
    public void parser(){

        JSONParser parser = new JSONParser();
        PriorityDb priorityDB;
        int priority_id = 0;
         
        try (FileReader fr = new FileReader(getPath()+getProjectName()+"/data_priority.json")) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String id = (String) jsonObject.get("id");
                String name = (String) jsonObject.get("name");
                
                priorityDB = new PriorityDb();
                int identifier = Integer.parseInt(id);
                priority_id = priorityDB.check_priority(identifier);
            
                if(priority_id == 0){

                    priorityDB.insert_priority(identifier, name);
                    
                }
                
            }
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }

}
    
