/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.DeveloperDb;
import util.BaseImport;
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
public class ImpDeveloper extends BaseImport{
    
    DeveloperDb devDB;
    
    public void parser(){

        BufferedReader br = null;
        PreparedStatement pstmt = null;
        Connection con = null;
        JSONParser parser = new JSONParser();
 
        try {
         
            Class.forName("nl.cwi.monetdb.jdbc.MonetDriver");
            con = DriverManager.getConnection(getUrl(), getUser(), getPassword());
            
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+getProject()+"/data_developer.json"));
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String name = (String) jsonObject.get("name");
                String display = (String) jsonObject.get("display_name");
                
                devDB = new DeveloperDb();
            project = pDB.check_project(this.getProject());
            
            if(project == 0){

                pDB.insert_project(this.getProject());
                project = pDB.check_project(this.getProject());
                    
            }
                
                String sql = "insert into gros.developer(name,display_name) values (?,?);";
                
                pstmt = con.prepareStatement(sql);
                
                pstmt.setString(1, name);
                pstmt.setString(2, display);
                
                pstmt.executeUpdate();
            }
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
        

}
    
