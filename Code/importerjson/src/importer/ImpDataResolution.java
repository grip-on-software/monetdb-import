/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import util.BaseImport;
import dao.ResolutionDb;
import java.io.FileReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Enrique
 */
public class ImpDataResolution extends BaseImport{
    
    @Override
    public void parser(){

        JSONParser parser = new JSONParser();
        ResolutionDb resolDb;
        int resol_id=0;
        String new_description="";
        String new_name="";
        
        try {
            
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+getProjectName()+"/data_resolution.json"));
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String description = (String) jsonObject.get("description");
                String id = (String) jsonObject.get("id");
                String name = (String) jsonObject.get("name");
                
                resolDb = new ResolutionDb();
                int identifier = Integer.parseInt(id);
                new_name = name.replace("'","\\'");
                new_description = description.replace("'","\\'");
                resol_id = resolDb.check_resolution(new_name);
                if(resol_id == 0){
                    resolDb.insert_resolution(identifier,new_name,new_description);
                    
                }
            }
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
        

}
    
