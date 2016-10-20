/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.DataSource;
import util.BaseImport;
import dao.DataTypeDb;
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
public class ImpDataType extends BaseImport{
    
    public void parser(String projectN){

        BufferedReader br = null;
        PreparedStatement pstmt = null;
        Connection con = null;
        JSONParser parser = new JSONParser();
        DataTypeDb dataTypeDb;
        int type_id = 0;
        
        //JSONParser parser = new JSONParser();
 
        try {
            con = DataSource.getInstance().getConnection();
            
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+projectN+"/data_type.json"));
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String description = (String) jsonObject.get("description");
                String id = (String) jsonObject.get("id");
                String name = (String) jsonObject.get("name");
                
                dataTypeDb = new DataTypeDb();
                type_id = dataTypeDb.check_issueType(Integer.parseInt(id));
            
                if(type_id == 0){

                    dataTypeDb.insert_issueType(name,description);
                    
                }
            }
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
        

}
    
