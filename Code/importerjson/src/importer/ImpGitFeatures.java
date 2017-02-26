/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import util.BaseImport;
import dao.GitFeatureDb;
import java.io.FileReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Enrique
 */
public class ImpGitFeatures extends BaseImport{
    
    @Override
    public void parser(){

        JSONParser parser = new JSONParser();
        GitFeatureDb gitFeatureDb = new GitFeatureDb();

        int user_id = 0;
 
        try (FileReader fr = new FileReader(getPath()+getProjectName()+"/data_features.json")) {
            
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String feature_name = (String) jsonObject.get("feature_name");
                String feature_value = (String) jsonObject.get("feature_value");
                String sprint_id = (String) jsonObject.get("sprint_id");
                String user_name = (String) jsonObject.get("user_name");
                
                user_id = gitFeatureDb.check_username(user_name);
            
                if(user_id == 0){
                    gitFeatureDb.insert_feature(feature_name,Double.parseDouble(feature_value),Integer.parseInt(sprint_id),user_name);                    
                } else{                
                    gitFeatureDb.insert_feature(feature_name,Double.parseDouble(feature_value),Integer.parseInt(sprint_id),user_id,user_name);                    
                }
            }
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
        

}
    
