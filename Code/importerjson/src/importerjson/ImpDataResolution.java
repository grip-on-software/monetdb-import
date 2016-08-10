/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importerjson;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Enrique
 */
public class ImpDataResolution {
    
    public void parser(){

        BufferedReader br = null;
        PreparedStatement pstmt = null;
        Connection con = null;
        JSONParser parser = new JSONParser();
        
        //JSONParser parser = new JSONParser();
 
        try {
 
            
            Class.forName("nl.cwi.monetdb.jdbc.MonetDriver");
            con = DriverManager.getConnection("jdbc:monetdb://MONETDB_SERVER.localhost:50000/gros", "monetdb", "monetdb");
 
            JSONArray a = (JSONArray) parser.parse(new FileReader("/path/to/data.json"));
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String description = (String) jsonObject.get("description");
                String id = (String) jsonObject.get("id");
                String name = (String) jsonObject.get("name");
                
                String sql = "insert into gros.resolution values (?,?,?);";
                
                pstmt = con.prepareStatement(sql);
                
                pstmt.setInt(1, Integer.parseInt(id));
                pstmt.setString(2, name);
                pstmt.setString(3, description);
                
                pstmt.executeUpdate();
            }
            //con.commit(); 
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
        

}
    
