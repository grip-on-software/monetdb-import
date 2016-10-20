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
public class StatusDb extends BaseImport{
    
    public void insert_status(String name, String desc){
        
        Connection con = null;
        Statement st = null;
        String sql="";
    
        try {
            con = DataSource.getInstance().getConnection();
       
            st = con.createStatement();
            sql = "insert into gros.status(name,description) values ('"+name+"','"+desc+"');";
                    
            st.executeUpdate(sql);

        } catch (SQLException | IOException | PropertyVetoException ex) {
            Logger.getLogger(StatusDb.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
    
    }
   
    public int check_status(String name){

        int idStatus = 0;
        Connection con = null;
        Statement st = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            con = DataSource.getInstance().getConnection();
            
            st = con.createStatement();
            String sql_var = "SELECT id FROM gros.status WHERE UPPER(name) = '" + name.toUpperCase().trim()+ "'";
            rs = st.executeQuery(sql_var);
 
            while (rs.next()) {
                idStatus = rs.getInt("id");
            }
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
        return idStatus;
    }
    
    public int check_status(int id){

        int idStatus = 0;
        Connection con = null;
        Statement st = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            con = DataSource.getInstance().getConnection();
            
            st = con.createStatement();
            String sql_var = "SELECT count(id) FROM gros.status WHERE id = " + id;
            rs = st.executeQuery(sql_var);
 
            while (rs.next()) {
                idStatus = rs.getInt(1);
            }
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
        return idStatus;
    }
        
}
    
