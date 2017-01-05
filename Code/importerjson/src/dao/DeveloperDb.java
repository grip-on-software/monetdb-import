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
public class DeveloperDb extends BaseImport{
    
    /**
     * Inserts developer in the developer table. 
     * @param name the username in Jira
     * @param display_name The complete name of the user
     */
    public void insert_developer(String name, String display_name){
        
        Connection con = null;
        Statement st = null;
        String sql="";
    
        try {
            con = DataSource.getInstance().getConnection();
       
            st = con.createStatement();
            sql = "insert into gros.developer (name,display_name) values ('"+name+"','"+display_name+"');";
                    
            st.executeUpdate(sql);

        } catch (SQLException | IOException | PropertyVetoException ex) {
            Logger.getLogger(DeveloperDb.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (st != null) try { st.close(); } catch (SQLException e) {e.printStackTrace();}
            if (con != null) try { con.close(); } catch (SQLException e) {e.printStackTrace();}
        }
        
    
    }
   /**
    * Returns the developer ID if the developer already exists in the developer 
    * table of the database. Else returns 0.
    * @param display_name the complete name of the developer in Jira.
    * @return the Developer ID if found, otherwise 0.
    */
    public int check_developer(String display_name){
        int idDeveloper = 0;
        Connection con = null;
        Statement st = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {

            Class.forName("nl.cwi.monetdb.jdbc.MonetDriver");
            con = DriverManager.getConnection(getUrl(), getUser(), getPassword());
            
            st = con.createStatement();
            String sql_var = "SELECT id FROM gros.developer WHERE UPPER(display_name) = '" + display_name.toUpperCase().trim()+ "'";
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
    
    /**
     * Inserts developers in the git developer table of the database. In case developer
     * id is not set, the developer id from Jira will be 0. The alias id's are initialized
     * incremental by the database.
     * @param dev_id the corresponding developer id in Jira 
     * @param display_name the full name of the user on Git.
     */
    public void insert_developer_git(int dev_id, String display_name){
        
        Connection con = null;
        Statement st = null;
        String sql="";
    
        try {
            con = DataSource.getInstance().getConnection();
       
            st = con.createStatement();
            sql = "insert into gros.git_developer (jira_dev_id, display_name) values ("+dev_id+",'"+display_name+"');";
                    
            st.executeUpdate(sql);
            
        } catch (SQLException | IOException | PropertyVetoException ex) {
            Logger.getLogger(DeveloperDb.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (st != null) try { st.close(); } catch (SQLException e) {e.printStackTrace();}
            if (con != null) try { con.close(); } catch (SQLException e) {e.printStackTrace();}
        }
        
    
    }
   
    /**
    * Returns the Alias ID if the developer already exists in the git developer 
    * table of the database. Else returns 0.
    * @param display_name the complete name of the developer in GIT.
    * @return the Developer ID if found, otherwise 0.
    */
    public int check_developer_git(String display_name){

        int idDeveloper = 0;
        Connection con = null;
        Statement st = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            con = DataSource.getInstance().getConnection();
            
            st = con.createStatement();
            String sql_var = "SELECT alias_id FROM gros.git_developer WHERE UPPER(display_name) = '" + display_name.toUpperCase().trim()+ "'";
            rs = st.executeQuery(sql_var);
 
            while (rs.next()) {
                idDeveloper = rs.getInt("alias_id");
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
    
    /**
     * Updates the entire commit table with the right jira dev id's instead of
     * the alias ids. ONLY RUN THIS IF THE GIT DEVELOPER TABLE IS COMPLETELY UPDATED!
     */
    public void updateCommits() {
        Connection con = null;
        Statement st = null;
        Statement st2 = null;
        ResultSet rs = null;
        ResultSet rs2 = null;
        
        try {
            con = DataSource.getInstance().getConnection();
            
            st = con.createStatement();
            String sql_var = "SELECT c.commit_id, c.developer_id, g.alias_id, g.jira_dev_id" +
                            "FROM gros.commits c, gros.git_developer g" +
                            "WHERE c.developer_id = g.alias_id;";
            rs = st.executeQuery(sql_var);
 
            while (rs.next()) {
                String commit_id = rs.getString("c.commit_id");
                int jira_id = rs.getInt("g.jira_dev_id");
                st2 = con.createStatement();
                String sql = "UPDATE gros.commits SET developer_id="+jira_id+" WHERE commit_id="+commit_id+";";
                st2.executeQuery(sql);
            }
        }
            
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {e.printStackTrace();}
            if (rs2 != null) try { rs.close(); } catch (SQLException e) {e.printStackTrace();}
            if (st != null) try { st.close(); } catch (SQLException e) {e.printStackTrace();}
            if (st2 != null) try { st.close(); } catch (SQLException e) {e.printStackTrace();}
            if (con != null) try { con.close(); } catch (SQLException e) {e.printStackTrace();}
        }
    }   
    
}
    
