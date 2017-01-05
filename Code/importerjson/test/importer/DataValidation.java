/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import dao.CommentDb;
import dao.DataSource;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;

/**
 *
 * @author Thomas
 */
public class DataValidation {
    
    
    
    
    /**
     * Test to check whether data is valid and everything is linked.
     */
    @Test
    public void testMain() throws IOException, SQLException, PropertyVetoException {
        String[] args = {"PROJ"};
        Importerjson.main(args);
        
        Connection con = null;
        Statement st = null;
        String sql="";
        ResultSet rs = null;
        
        try {
            con = DataSource.getInstance().getConnection();
            st = con.createStatement();
            
            String sql_project = "SELECT * FROM gros.projects p";
            st = con.createStatement();
            rs = st.executeQuery(sql_project);
 
            while (rs.next()) {
                int project_id = rs.getInt("projectid");
                
                String sql_issues = "SELECT * FROM gros.issues WHERE project_id=" + project_id;
                Statement st2 = con.createStatement();
                ResultSet rs2 = st.executeQuery(sql_issues);
                
                // Resolution
                // assignee
                // reporter
                // priority
                // sprint_id
                // updated_by 
                // How issues are linked (subtask, etc, etc)
                // Relationship --> Relationship
                // status
                while(rs2.next()) {
                    int resolution = rs2.getInt("resolution");
                    
                    String assignee = rs2.getString("assignee");
                    String reporter = rs2.getString("reporter");
                    String updated_by = rs2.getString("updated_by");
                    
                    int priority = rs2.getInt("priority");
                    int sprint_id = rs2.getInt("sprint_id");
                    
                    Statement st3 = con.createStatement();
                    String sql_check = "SELECT * FROM gros.resolution WHERE id=" + resolution;
                    ResultSet rs3 = st3.executeQuery(sql_check);
                    
                    int count = 0;
                    while(rs3.next()) {
                        count += 1;
                    }
                    if (count > 1) {
                        System.out.println("Something goes wrong, too many resolution types found!");
                    }
                    
                    st3 = con.createStatement();
                    sql_check = "SELECT * FROM gros.developer WHERE name=" + reporter;
                    rs3 = st3.executeQuery(sql_check);
                    
                    count = 0;
                    while(rs3.next()) {
                        count += 1;
                    }
                    if (count > 1 || count < 1) {
                        System.out.println("Something goes wrong, too many reporters found!");
                    }
                    
                    st3 = con.createStatement();
                    sql_check = "SELECT * FROM gros.developer WHERE name=" + assignee;
                    rs3 = st3.executeQuery(sql_check);
                    
                    count = 0;
                    while(rs3.next()) {
                        count += 1;
                    }
                    if (count > 1 || count < 1) {
                        System.out.println("Something goes wrong, too many reporters found!");
                    }
                    
                    
                }
                
                
            }
            
                    
            
            
        } catch (SQLException | IOException ex) {
            Logger.getLogger(CommentDb.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (st != null) try { st.close(); } catch (SQLException e) {e.printStackTrace();}
            if (con != null) try { con.close(); } catch (SQLException e) {e.printStackTrace();}
        }

 
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }
}
