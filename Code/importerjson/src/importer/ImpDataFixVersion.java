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
import java.sql.Date;
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
public class ImpDataFixVersion extends BaseImport{
    
    @Override
    public void parser() {

        BufferedReader br = null;
        PreparedStatement existsStmt = null;
        PreparedStatement pstmt = null;
        Connection con = null;
        JSONParser parser = new JSONParser();
        Statement st = null;
        ResultSet rs = null;
        String sql = "";
         
        try (FileReader fr = new FileReader(getPath()+getProjectName()+"/data_fixVersion.json")) {
            con = DataSource.getInstance().getConnection();

            sql = "SELECT * FROM gros.fixversion WHERE id=?;";
            existsStmt = con.prepareStatement(sql);

            sql = "insert into gros.fixversion values (?,?,?,?,?);";
            pstmt = con.prepareStatement(sql);
            
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
                JSONObject jsonObject = (JSONObject) o;
                
                String description = (String) jsonObject.get("description");
                String id = (String) jsonObject.get("id");
                String name = (String) jsonObject.get("name");
                String release_date = (String) jsonObject.get("release_date");
                String released = (String) jsonObject.get("released");
                
                existsStmt.setInt(1, Integer.parseInt(id));
                rs = existsStmt.executeQuery();
                // check whether a row with the given ID does not already exist
                if (!rs.next()) {
                    pstmt.setInt(1, Integer.parseInt(id));
                    pstmt.setString(2, name);
                    pstmt.setString(3, description);

                    if ((release_date.trim()).equals("0")){
                        release_date = null;
                    }

                    Date d_rdate;              
                    if (release_date !=null){
                        d_rdate = Date.valueOf(release_date); 
                        pstmt.setDate(4, d_rdate);
                    } else{
                        //release_date = null;
                        pstmt.setNull(4, java.sql.Types.DATE);
                    }
                    
                    if (released == null || released.equals("0")) {
                        pstmt.setNull(5, java.sql.Types.BOOLEAN);
                    }
                    else {
                        pstmt.setBoolean(5, released.equals("1"));
                    }

                    pstmt.executeUpdate();
                }
            }
        }            
        catch (Exception ex) {
            logException(ex);
        } finally {
            if (con != null) try { con.close(); } catch (SQLException ex) {logException(ex);}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException ex) {logException(ex);}
            if (existsStmt != null) try { existsStmt.close(); } catch (SQLException ex) {logException(ex);}
            if (st != null) try { st.close(); } catch (SQLException ex) {logException(ex);}
            if (rs != null) try { rs.close(); } catch (SQLException ex) {logException(ex);}
        }
        
    }
        

}
    
