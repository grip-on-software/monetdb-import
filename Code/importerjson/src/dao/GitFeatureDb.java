/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import java.beans.PropertyVetoException;
import java.io.IOException;
import util.BaseImport;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrique
 */
public class GitFeatureDb extends BaseImport{
    
    public void insert_feature(String name, Double value, int sprint, String user){
        
        Connection con = null;
        Statement st = null;
        String sql="";
    
        try {
            con = DataSource.getInstance().getConnection();
       
            st = con.createStatement();
            sql = "insert into gros.git_features (feature_name,feature_value,sprint_id,user_name) values ('"+name+"',"+value+","+sprint+"'"+user+"'');";
                    
            st.executeUpdate(sql);

        } catch (SQLException | IOException | PropertyVetoException ex) {
            Logger.getLogger(GitFeatureDb.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (st != null) try { st.close(); } catch (SQLException e) {e.printStackTrace();}
            if (con != null) try { con.close(); } catch (SQLException e) {e.printStackTrace();}
        }
         
    
    }
   
    public void insert_feature(String name, Double value, int sprint, int userId, String user){
        
        Connection con = null;
        Statement st = null;
        String sql="";
    
        try {
            con = DataSource.getInstance().getConnection();
       
            st = con.createStatement();
            sql = "insert into gros.git_features (feature_name,feature_value,sprint_id,user_id,user_name) values ('"+name+"',"+value+","+sprint+","+userId+",'"+user+"'');";
                    
            st.executeUpdate(sql);

        } catch (SQLException | IOException | PropertyVetoException ex) {
            Logger.getLogger(GitFeatureDb.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (st != null) try { st.close(); } catch (SQLException e) {e.printStackTrace();}
            if (con != null) try { con.close(); } catch (SQLException e) {e.printStackTrace();}
        }
        
    
    }
    
    
    public int check_username(String name){

        int idUser = 0;
        Connection con = null;
        Statement st = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            con = DataSource.getInstance().getConnection();
            
            st = con.createStatement();
            String sql_var = "SELECT id FROM gros.developer WHERE UPPER(display_name) = '" + name.toUpperCase().trim()+ "'";
            rs = st.executeQuery(sql_var);
 
            while (rs.next()) {
                idUser = rs.getInt("id");
            }
            
        }
            
        catch (SQLException e) {
            printSQLExceptionDetails(e);
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {e.printStackTrace();}
            if (st != null) try { st.close(); } catch (SQLException e) {e.printStackTrace();}
            if (con != null) try { con.close(); } catch (SQLException e) {e.printStackTrace();}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {e.printStackTrace();}
        }
        
        return idUser;
    }
        
}
    
