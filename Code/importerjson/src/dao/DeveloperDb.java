/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

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
 *
 * @author Enrique
 */
public class DeveloperDb extends BaseImport{
    
    public void insert_developer(String name, String display_name){
        
        Connection con = null;
        Statement st = null;
        String sql="";
    
        try {
            Class.forName("nl.cwi.monetdb.jdbc.MonetDriver");
            con = DriverManager.getConnection(getUrl(), getUser(), getPassword());
       
            st = con.createStatement();
            sql = "insert into gros.developer (name,display_name) values ('"+name+"','"+display_name+"');";
                    
            st.executeUpdate(sql);
            
            con.close();
            
        } catch (SQLException ex) {
            Logger.getLogger(DeveloperDb.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(DeveloperDb.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    
    }
   
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
            
            con.close();
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
        return idDeveloper;
    } 
    
    public void insert_developer_git(int dev_id, String display_name){
        
        Connection con = null;
        Statement st = null;
        String sql="";
    
        try {
            Class.forName("nl.cwi.monetdb.jdbc.MonetDriver");
            con = DriverManager.getConnection(getUrl(), getUser(), getPassword());
       
            st = con.createStatement();
            sql = "insert into gros.git_developer (jira_dev_id, display_name) values ("+dev_id+",'"+display_name+"');";
                    
            st.executeUpdate(sql);
            
            con.close();
            
        } catch (SQLException ex) {
            Logger.getLogger(DeveloperDb.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(DeveloperDb.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    
    }
   
    public int check_developer_git(String display_name){

        int idDeveloper = 0;
        Connection con = null;
        Statement st = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {

            Class.forName("nl.cwi.monetdb.jdbc.MonetDriver");
            con = DriverManager.getConnection(getUrl(), getUser(), getPassword());
            
            st = con.createStatement();
            String sql_var = "SELECT alias_id FROM gros.git_developer WHERE UPPER(display_name) = '" + display_name.toUpperCase().trim()+ "'";
            rs = st.executeQuery(sql_var);
 
            while (rs.next()) {
                idDeveloper = rs.getInt("alias_id");
            }
            
            con.close();
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
        return idDeveloper;
    }   
    
    public void updateCommits() {
        Connection con = null;
        Statement st = null;
        Statement st2 = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ResultSet rs2 = null;
        
        try {

            Class.forName("nl.cwi.monetdb.jdbc.MonetDriver");
            con = DriverManager.getConnection(getUrl(), getUser(), getPassword());
            
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
            
            con.close();
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
    }   
    
}
    
