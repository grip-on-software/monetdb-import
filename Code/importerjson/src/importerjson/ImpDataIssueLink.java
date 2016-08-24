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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Enrique
 */
public class ImpDataIssueLink extends BaseImport{
    
    public void parser(){

        BufferedReader br = null;
        PreparedStatement pstmt = null;
        Connection con = null;
        JSONParser parser = new JSONParser();
        
        //JSONParser parser = new JSONParser();
 
        try {
 
            
            Class.forName("nl.cwi.monetdb.jdbc.MonetDriver");
            con = DriverManager.getConnection(getUrl(), getUser(), getPassword());
            
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+getProject()+"/data_issuelinks.json"));
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String to_id = (String) jsonObject.get("to_id");
                String relationshiptype = (String) jsonObject.get("relationshiptype");
                String from_id = (String) jsonObject.get("from_id");
                
                String sql = "insert into gros.issuelink values (?,?,?);";
                
                pstmt = con.prepareStatement(sql);
                
                pstmt.setInt(1, Integer.parseInt(to_id));
                pstmt.setInt(2, Integer.parseInt(relationshiptype));
                pstmt.setInt(3, Integer.parseInt(from_id));
                
              
                pstmt.executeUpdate();
            }
            //con.commit(); 
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
        

}
    
