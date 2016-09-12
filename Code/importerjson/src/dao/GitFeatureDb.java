/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import util.BaseImport;
import java.sql.Connection;
import java.sql.DriverManager;
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
            Class.forName("nl.cwi.monetdb.jdbc.MonetDriver");
            con = DriverManager.getConnection(getUrl(), getUser(), getPassword());
       
            st = con.createStatement();
            sql = "insert into gros.git_features (feature_name,feature_value,sprint_id,user_name) values ('"+name+"',"+value+","+sprint+"'"+user+"'');";
                    
            st.executeUpdate(sql);
            
            con.close();
            
        } catch (SQLException ex) {
            Logger.getLogger(GitFeatureDb.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(GitFeatureDb.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    
    }
   
    public void insert_feature(String name, Double value, int sprint, int userId, String user){
        
        Connection con = null;
        Statement st = null;
        String sql="";
    
        try {
            Class.forName("nl.cwi.monetdb.jdbc.MonetDriver");
            con = DriverManager.getConnection(getUrl(), getUser(), getPassword());
       
            st = con.createStatement();
            sql = "insert into gros.git_features (feature_name,feature_value,sprint_id,user_id,user_name) values ('"+name+"',"+value+","+sprint+","+userId+",'"+user+"'');";
                    
            st.executeUpdate(sql);
            
            con.close();
            
        } catch (SQLException ex) {
            Logger.getLogger(GitFeatureDb.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(GitFeatureDb.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    
    }
    
    
    public int check_username(String name){

        int idUser = 0;
        Connection con = null;
        Statement st = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {

            Class.forName("nl.cwi.monetdb.jdbc.MonetDriver");
            con = DriverManager.getConnection(getUrl(), getUser(), getPassword());
            
            st = con.createStatement();
            String sql_var = "SELECT id FROM gros.developer WHERE UPPER(display_name) = '" + name.toUpperCase().trim()+ "'";
            rs = st.executeQuery(sql_var);
 
            while (rs.next()) {
                idUser = rs.getInt("id");
            }
            
            con.close();
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
        return idUser;
    }
        
}
    
