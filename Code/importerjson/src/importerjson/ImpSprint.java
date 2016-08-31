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
import java.sql.Timestamp;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Enrique
 */
public class ImpSprint extends BaseImport{
    
    public void parser(){

        BufferedReader br = null;
        PreparedStatement pstmt = null;
        Connection con = null;
        JSONParser parser = new JSONParser();
 
        try {
         
            Class.forName("nl.cwi.monetdb.jdbc.MonetDriver");
            con = DriverManager.getConnection(getUrl(), getUser(), getPassword());
            
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+getProject()+"/data_sprint.json"));
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String id = (String) jsonObject.get("id");
                String name = (String) jsonObject.get("name");
                String start = (String) jsonObject.get("start_date");
                String end = (String) jsonObject.get("end_date");
                
                if ((start.trim()).equals("0") || (start.trim()).equals("None")){
                    start = null;
                }
                
                if ((end.trim()).equals("0") || (end.trim()).equals("None")){
                    end = null;
                }
                
                String sql = "insert into gros.sprint values (?,?,?,?,?);";
                
                pstmt = con.prepareStatement(sql);
                
                pstmt.setInt(1, Integer.parseInt(id));
                pstmt.setInt(2, this.getProjectID());
                pstmt.setString(3, name);
                
                Timestamp ts_start;              
                if (start !=null){
                    ts_start = Timestamp.valueOf(start); 
                    pstmt.setTimestamp(4,ts_start);
                } else{
                    //start = null;
                    pstmt.setNull(3, java.sql.Types.TIMESTAMP);
                }
                
                Timestamp ts_end;              
                if (end !=null){
                    ts_end = Timestamp.valueOf(end); 
                    pstmt.setTimestamp(5,ts_end);
                } else{
                    //end = null;
                    pstmt.setNull(4, java.sql.Types.TIMESTAMP);
                }
                
                pstmt.executeUpdate();
            }
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
        

}
    
