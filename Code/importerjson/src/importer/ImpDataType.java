/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import util.BaseImport;
import dao.DataTypeDb;
import java.io.FileReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Enrique
 */
public class ImpDataType extends BaseImport{
    
    @Override
    public void parser(){

        JSONParser parser = new JSONParser();
        DataTypeDb dataTypeDb;
        int type_id = 0;
        
        try (FileReader fr = new FileReader(getPath()+getProjectName()+"/data_type.json")) {
            
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String description = (String) jsonObject.get("description");
                String id = (String) jsonObject.get("id");
                String name = (String) jsonObject.get("name");
                
                int identifier = Integer.parseInt(id);
                
                dataTypeDb = new DataTypeDb();
                type_id = dataTypeDb.check_issueType(identifier);
            
                if(type_id == 0){

                    dataTypeDb.insert_issueType(identifier, name, description);
                    
                }
            }
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
        

}
    
