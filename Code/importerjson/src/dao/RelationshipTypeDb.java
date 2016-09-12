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
public class RelationshipTypeDb extends BaseImport{
    
    public void insert_relType(String name){
        
        Connection con = null;
        Statement st = null;
        String sql="";
    
        try {
            Class.forName("nl.cwi.monetdb.jdbc.MonetDriver");
            con = DriverManager.getConnection(getUrl(), getUser(), getPassword());
       
            st = con.createStatement();
            sql = "insert into gros.relationshiptype(name) values ('"+name+"');";
                    
            st.executeUpdate(sql);
            
            con.close();
            
        } catch (SQLException ex) {
            Logger.getLogger(RelationshipTypeDb.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(RelationshipTypeDb.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    
    }
   
    public int check_relType(String name){

        int idRel = 0;
        Connection con = null;
        Statement st = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {

            Class.forName("nl.cwi.monetdb.jdbc.MonetDriver");
            con = DriverManager.getConnection(getUrl(), getUser(), getPassword());
            
            st = con.createStatement();
            String sql_var = "SELECT id FROM gros.relationshiptype WHERE UPPER(name) = '" + name.toUpperCase().trim()+ "'";
            rs = st.executeQuery(sql_var);
 
            while (rs.next()) {
                idRel = rs.getInt("id");
            }
            
            con.close();
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
        return idRel;
    }
    
    public int check_relType(int id){

        int idRel = 0;
        Connection con = null;
        Statement st = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {

            Class.forName("nl.cwi.monetdb.jdbc.MonetDriver");
            con = DriverManager.getConnection(getUrl(), getUser(), getPassword());
            
            st = con.createStatement();
            String sql_var = "SELECT count(id) FROM gros.relationshiptype WHERE id = " + id;
            rs = st.executeQuery(sql_var);
 
            while (rs.next()) {
                idRel = rs.getInt(1);
            }
            
            con.close();
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
        return idRel;
    }
        
}
    
