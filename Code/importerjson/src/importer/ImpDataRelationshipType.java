/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.DataSource;
import util.BaseImport;
import dao.RelationshipTypeDb;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Enrique
 */
public class ImpDataRelationshipType extends BaseImport{
    
    public void parser(String projectN){

        BufferedReader br = null;
        PreparedStatement pstmt = null;
        Connection con = null;
        JSONParser parser = new JSONParser();
        RelationshipTypeDb relTypeDB;
        int rel_id = 0;
        
        //JSONParser parser = new JSONParser();
 
        try {
            con = DataSource.getInstance().getConnection();
            
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+projectN+"/data_relationshiptype.json"));
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String id = (String) jsonObject.get("id");
                String name = (String) jsonObject.get("name");
                
                relTypeDB = new RelationshipTypeDb();
                rel_id = relTypeDB.check_relType(Integer.parseInt(id));
            
                if(rel_id == 0){

                    relTypeDB.insert_relType(name);
                    
                }
            }
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {e.printStackTrace();}
            if (con != null) try { con.close(); } catch (SQLException e) {e.printStackTrace();}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {e.printStackTrace();}
        }
        
    }
        

}
    
