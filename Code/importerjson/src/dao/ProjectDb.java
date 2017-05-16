/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import java.beans.PropertyVetoException;
import util.BaseDb;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author Enrique
 */
public class ProjectDb extends BaseDb {
    
    public void insert_project(String name){
        
        Connection con = null;
        Statement st = null;
    
        try {
            con = DataSource.getInstance().getConnection();
       
            st = con.createStatement();
            String sql = "insert into gros.project(name) values ('"+name+"');";
                    
            st.executeUpdate(sql);
        } catch (SQLException | PropertyVetoException ex) {
            logException(ex);
        } finally {
            if (st != null) try { st.close(); } catch (SQLException ex) {logException(ex);}
            if (con != null) try { con.close(); } catch (SQLException ex) {logException(ex);}
        }
        
    
    }
   
    public int check_project(String name){

        int idProject = 0;
        Connection con = null;
        Statement st = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            con = DataSource.getInstance().getConnection();
            
            st = con.createStatement();
            String sql_var = "SELECT project_id FROM gros.project WHERE UPPER(name) = '" + name.toUpperCase().trim()+ "'";
            rs = st.executeQuery(sql_var);
 
            while (rs.next()) {
                idProject = rs.getInt("project_id");
            }

        }
        catch (PropertyVetoException | SQLException ex) {
            logException(ex);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ex) {logException(ex);}
            if (st != null) try { st.close(); } catch (SQLException ex) {logException(ex);}
            if (con != null) try { con.close(); } catch (SQLException ex) {logException(ex);}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException ex) {logException(ex);}
        }
        
        return idProject;
    }
        

}
    
