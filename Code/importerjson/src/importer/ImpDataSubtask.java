/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.BatchedStatement;
import dao.DataSource;
import util.BaseImport;
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
public class ImpDataSubtask extends BaseImport{
    
    @Override
    public void parser() {

        BatchedStatement bstmt = null;
        PreparedStatement pstmt = null;
        PreparedStatement existsStmt = null;
        Connection con = null;
        JSONParser parser = new JSONParser();
        Statement st = null;
        ResultSet rs = null;
 
        try {
            con = DataSource.getInstance().getConnection();
            String sql = "SELECT * FROM gros.subtask WHERE id_parent=? AND id_subtask=?";
            existsStmt = con.prepareStatement(sql);

            sql = "insert into gros.subtask values (?,?);";
            bstmt = new BatchedStatement(sql);
            pstmt = bstmt.getPreparedStatement();
            
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+getProjectName()+"/data_subtasks.json"));
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String from_id = (String) jsonObject.get("from_id");
                String to_id = (String) jsonObject.get("to_id");
                
                existsStmt.setInt(1, Integer.parseInt(from_id));
                existsStmt.setInt(2, Integer.parseInt(to_id));
                
                rs = existsStmt.executeQuery();
                
                // check if the link does not already exist in the database
                if(!rs.next()) {
                    pstmt.setInt(1, Integer.parseInt(from_id));
                    pstmt.setInt(2, Integer.parseInt(to_id));

                    bstmt.batch();
                }
            }
            bstmt.execute();
        }
            
        catch (SQLException e) {
            printSQLExceptionDetails(e);
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bstmt != null) { bstmt.close(); }
            if (con != null) try { con.close(); } catch (SQLException e) {e.printStackTrace();}
            if (existsStmt != null) try { existsStmt.close(); } catch (SQLException e) {e.printStackTrace();}
            if (rs != null) try { rs.close(); } catch (SQLException e) {e.printStackTrace();}
            if (st != null) try { st.close(); } catch (SQLException e) {e.printStackTrace();}
        }
        
    }
        

}