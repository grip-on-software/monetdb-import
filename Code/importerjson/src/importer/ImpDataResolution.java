/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.DataSource;
import util.BaseImport;
import dao.ResolutionDb;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Enrique
 */
public class ImpDataResolution extends BaseImport{
    
    public void parser(String projectN){

        BufferedReader br = null;
        PreparedStatement pstmt = null;
        Connection con = null;
        JSONParser parser = new JSONParser();
        ResolutionDb resolDb;
        int resol_id=0;
        String new_description="";
        String new_name="";
        
        //JSONParser parser = new JSONParser();
 
        try {
            con = DataSource.getInstance().getConnection();
            
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+projectN+"/data_resolution.json"));
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String description = (String) jsonObject.get("description");
                String id = (String) jsonObject.get("id");
                String name = (String) jsonObject.get("name");
                
                resolDb = new ResolutionDb();
                int identifier = Integer.parseInt(id);
                resol_id = resolDb.check_resolution(identifier);
                System.out.println(id + ", " + name + ", " + description);
                if(resol_id == 0){

                    new_name = name.replace("'","\\'");
                    new_description = description.replace("'","\\'");
                    resolDb.insert_resolution(identifier,new_name,new_description);
                    
                }
            }
            //con.commit(); 
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
        

}
    
