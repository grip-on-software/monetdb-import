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
public class ResolutionDb extends BaseImport{
    
    public void insert_resolution(int id, String name, String desc){
        
        Connection con = null;
        Statement st = null;
        String sql="";
    
        try {
            con = DataSource.getInstance().getConnection();
       
            st = con.createStatement();
            sql = "insert into gros.resolution (id,name,description) values ('"+id+"' ,'"+name+"','"+desc+"');";
                    
            st.executeUpdate(sql);
            
        } catch (SQLException | IOException | PropertyVetoException ex) {
            Logger.getLogger(ResolutionDb.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (st != null) try { st.close(); } catch (SQLException e) {e.printStackTrace();}
            if (con != null) try { con.close(); } catch (SQLException e) {e.printStackTrace();}
        }
    }
   
    public int check_resolution(String name){

        int idResol = 0;
        Connection con = null;
        Statement st = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            con = DataSource.getInstance().getConnection();
            
            st = con.createStatement();
            String sql_var = "SELECT id FROM gros.resolution WHERE UPPER(name) = '" + name.toUpperCase().trim()+ "'";
            rs = st.executeQuery(sql_var);
 
            while (rs.next()) {
                idResol = rs.getInt("id");
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
        
        return idResol;
    }
    
    public int check_resolution(int id){

        int idResol = 0;
        Connection con = null;
        Statement st = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            con = DataSource.getInstance().getConnection();
            
            st = con.createStatement();
            String sql_var = "SELECT count(id) FROM gros.resolution WHERE id = " + id;
            rs = st.executeQuery(sql_var);
 
            while (rs.next()) {
                idResol = rs.getInt(1);
            }

        }
            
        catch (SQLException e) {
            printSQLExceptionDetails(e);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        return idResol;
    }
        
}
    
