/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import util.BaseImport;
import dao.GitFeatureDb;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Enrique
 */
public class ImpGitFeatures extends BaseImport{
    
    public void parser(String projectN){

        BufferedReader br = null;
        PreparedStatement pstmt = null;
        Connection con = null;
        JSONParser parser = new JSONParser();
        GitFeatureDb gitFeatureDb;
        int user_id = 0;
 
        try {
 
            
            Class.forName("nl.cwi.monetdb.jdbc.MonetDriver");
            con = DriverManager.getConnection(getUrl(), getUser(), getPassword());
            
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+projectN+"/data_features.json"));
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String feature_name = (String) jsonObject.get("feature_name");
                String feature_value = (String) jsonObject.get("feature_value");
                String sprint_id = (String) jsonObject.get("sprint_id");
                String user_name = (String) jsonObject.get("user_name");
                
                gitFeatureDb = new GitFeatureDb();
                user_id = gitFeatureDb.check_username(user_name.trim());
            
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
    
