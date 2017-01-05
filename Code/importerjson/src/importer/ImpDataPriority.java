/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.DataSource;
import util.BaseImport;
import dao.PriorityDb;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Enrique
 */
public class ImpDataPriority extends BaseImport{
    
    public void parser(String projectN){

        BufferedReader br = null;
        JSONParser parser = new JSONParser();
        PriorityDb priorityDB;
        int priority_id = 0;
        
        //JSONParser parser = new JSONParser();
 
        try {
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+projectN+"/data_priority.json"));
            
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
    
    public int check_priority(String name){

        int idPriority = 0;
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        
        try {
            con = DataSource.getInstance().getConnection();
            
            st = con.createStatement();
            String sql_var = "SELECT id FROM gros.priority WHERE UPPER(name) = '" + name.toUpperCase().trim()+ "'";
            rs = st.executeQuery(sql_var);
 
            while (rs.next()) {
                idPriority = rs.getInt("id");
            }

        }
            
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (st != null) try { st.close(); } catch (SQLException e) {e.printStackTrace();}
            if (con != null) try { con.close(); } catch (SQLException e) {e.printStackTrace();}
        }
        
        return idPriority;
    }
        

}
    
