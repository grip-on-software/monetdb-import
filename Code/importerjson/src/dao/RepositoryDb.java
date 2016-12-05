/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.BaseImport;

/**
 * Class is created to manage the developer table of the database.
 * @author Enrique & Thomas
 */
public class RepositoryDb extends BaseImport{
    
    /**
     * Inserts repository in the developer table. 
     * @param name The complete name of the repository.
     */
    public void insert_repo(String name){
        
        Connection con = null;
        Statement st = null;
        String sql="";
    
        try {
            con = DataSource.getInstance().getConnection();
       
            st = con.createStatement();
            sql = "insert into gros.git_repo (name) values ('"+name+"');";
                    
            st.executeUpdate(sql);

        } catch (SQLException | IOException | PropertyVetoException ex) {
            Logger.getLogger(RepositoryDb.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (st != null) try { st.close(); } catch (SQLException e) {e.printStackTrace();}
            if (con != null) try { con.close(); } catch (SQLException e) {e.printStackTrace();}
        }
        
    
    }
   /**
    * Returns the developer ID if the developer already exists in the developer 
    * table of the database. Else returns 0.
    * @param name the complete name of the repository.
    * @return the Developer ID if found, otherwise 0.
    */
    public int check_repo(String name){
        int idDeveloper = 0;
        Connection con = null;
        Statement st = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {

            Class.forName("nl.cwi.monetdb.jdbc.MonetDriver");
            con = DriverManager.getConnection(getUrl(), getUser(), getPassword());
            
            st = con.createStatement();
            String sql_var = "SELECT id FROM gros.git_repo WHERE UPPER(name) = '" + name.toUpperCase().trim()+ "'";
            rs = st.executeQuery(sql_var);
 
            while (rs.next()) {
                idDeveloper = rs.getInt("id");
            }

        }
            
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {e.printStackTrace();}
            if (st != null) try { st.close(); } catch (SQLException e) {e.printStackTrace();}
            if (con != null) try { con.close(); } catch (SQLException e) {e.printStackTrace();}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {e.printStackTrace();}
        }
        
        return idDeveloper;
    } 
    
}
    
