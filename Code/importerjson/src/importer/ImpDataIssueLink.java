/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.DataSource;
import util.BaseImport;
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
public class ImpDataIssueLink extends BaseImport{
    
    public void parser(String projectN){

        BufferedReader br = null;
        PreparedStatement pstmt = null;
        Connection con = null;
        JSONParser parser = new JSONParser();
        Statement st = null;
        ResultSet rs = null;
        //JSONParser parser = new JSONParser();
 
        try {
            con = DataSource.getInstance().getConnection();
            
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+projectN+"/data_issuelinks.json"));
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String to_id = (String) jsonObject.get("to_id");
                String relationshiptype = (String) jsonObject.get("relationshiptype");
                String from_id = (String) jsonObject.get("from_id");
                
                st = con.createStatement();
                String sql = "SELECT * FROM gros.issuelink WHERE id_from=" + Integer.parseInt(from_id) + " AND "
                        + "id_to=" + Integer.parseInt(to_id) + " AND relationship_type=" + Integer.parseInt(relationshiptype);
                rs = st.executeQuery(sql);
                boolean exists = false;
                while (rs.next()) {
                    exists=true;
                }
                
                if(!exists) {
                    sql = "insert into gros.issuelink values (?,?,?);";

                    pstmt = con.prepareStatement(sql);

                    pstmt.setInt(1, Integer.parseInt(from_id));
                    pstmt.setInt(2, Integer.parseInt(to_id));
                    pstmt.setInt(3, Integer.parseInt(relationshiptype));

                    pstmt.executeUpdate();
                }
            }
            //con.commit(); 
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (con != null) try { con.close(); } catch (SQLException e) {e.printStackTrace();}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {e.printStackTrace();}
            if (rs != null) try { rs.close(); } catch (SQLException e) {e.printStackTrace();}
            if (st != null) try { st.close(); } catch (SQLException e) {e.printStackTrace();}
        }
        
    }
        

}
    
