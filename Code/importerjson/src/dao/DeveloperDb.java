/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import util.BaseDb;

/**
 * Class is created to manage the developer table of the database.
 * @author Enrique & Thomas
 */
public class DeveloperDb extends BaseDb implements AutoCloseable {
    
    PreparedStatement checkDeveloperStmt = null;
    PreparedStatement checkVcsDeveloperStmt = null;
    PreparedStatement insertVcsDeveloperStmt = null;
    BatchedStatement bstmt = null;
    HashMap<String, Integer> vcsNameCache = null;
    
    public DeveloperDb() {
        String sql = "insert into gros.developer (name,display_name) values (?,?);";
        bstmt = new BatchedStatement(sql);
    }
    
    /**
     * Inserts developer in the developer table. 
     * @param name the username in Jira
     * @param display_name The complete name of the user
     */
    public void insert_developer(String name, String display_name) throws SQLException, IOException, PropertyVetoException{
        PreparedStatement pstmt = bstmt.getPreparedStatement();
        
        pstmt.setString(1, name);
        pstmt.setString(2, display_name);
        
        // Insert immediately because we need to have the row available
        pstmt.execute();
    }
    
    /**
     * Commits changes to the developer table and closes the connection.
     * @throws java.sql.SQLException
     */
    @Override
    public void close() throws SQLException {
        bstmt.execute();
        bstmt.close();
        
        if (checkDeveloperStmt != null) {
            checkDeveloperStmt.close();
        }
        if (insertVcsDeveloperStmt != null) {
            insertVcsDeveloperStmt.close();
        }
        if (checkVcsDeveloperStmt != null) {
            checkVcsDeveloperStmt.close();
        }
        
        if (vcsNameCache != null) {
            vcsNameCache.clear();
            vcsNameCache = null;
        }
    }
    
    private void getCheckDeveloperStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkDeveloperStmt == null) {
            Connection con = bstmt.getConnection();
            String sql = "SELECT id FROM gros.developer WHERE UPPER(name) = ? OR UPPER(display_name) = ?";
            checkDeveloperStmt = con.prepareStatement(sql);
        }
    }
    
    /**
     * Returns the developer ID if the developer already exists in the developer 
     * table of the database. Else returns 0.
     * @param name the short alias of the developer in Jira.
     * @param display_name the complete name of the developer in Jira.
     * @return the Developer ID if found, otherwise 0.
     */
    public int check_developer(String name, String display_name) throws SQLException, IOException, PropertyVetoException {
        int idDeveloper = 0;
        getCheckDeveloperStmt();
        ResultSet rs = null;
        
        checkDeveloperStmt.setString(1, name.toUpperCase().trim());
        checkDeveloperStmt.setString(2, display_name.toUpperCase().trim());
        rs = checkDeveloperStmt.executeQuery();
 
        while (rs.next()) {
            idDeveloper = rs.getInt("id");
        }
        
        rs.close();
        
        return idDeveloper;
    } 
    
    private void getInsertVcsDeveloperStmt() throws SQLException, IOException, PropertyVetoException {
        if (insertVcsDeveloperStmt == null) {
            Connection con = bstmt.getConnection();
            String sql = "insert into gros.vcs_developer (jira_dev_id, display_name) values (?,?);";
            insertVcsDeveloperStmt = con.prepareStatement(sql);
        } 
    }
    
    /**
     * Inserts developers in the VCS developer table of the database. In case developer
     * id is not set, the developer id from Jira will be 0. The alias id's are initialized
     * incremental by the database.
     * @param dev_id the corresponding developer id in Jira 
     * @param display_name the full name of the user in the version control system.
     */
    public void insert_vcs_developer(int dev_id, String display_name) throws SQLException, IOException, PropertyVetoException{
        getInsertVcsDeveloperStmt();
        
        insertVcsDeveloperStmt.setInt(1, dev_id);
        insertVcsDeveloperStmt.setString(2, display_name);
    
        insertVcsDeveloperStmt.execute();
    }
   
    private void getCheckVcsDeveloperStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkVcsDeveloperStmt == null) {
            Connection con = bstmt.getConnection();
            String sql = "SELECT alias_id FROM gros.vcs_developer WHERE UPPER(display_name) = ?";
            checkVcsDeveloperStmt = con.prepareStatement(sql);
        }
    }
    
    private void fillVcsNameCache() throws SQLException, IOException, PropertyVetoException {
        if (vcsNameCache != null) {
            return;
        }
        vcsNameCache = new HashMap<>();
        
        Connection con = bstmt.getConnection();
        String sql = "SELECT UPPER(display_name), alias_id FROM gros.vcs_developer";
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        while(rs.next()) {
            String key = rs.getString(1);
            Integer id = Integer.parseInt(rs.getString(2));
            vcsNameCache.put(key, id);
        }

        stmt.close();
        rs.close();
    }
    
    /**
    * Returns the Alias ID if the developer already exists in the VCS developer 
    * table of the database. Else returns 0.
    * @param display_name the complete name of the developer in the version control system.
    * @return the Developer ID if found, otherwise 0.
    */
    public int check_vcs_developer(String display_name) throws SQLException, IOException, PropertyVetoException {
        fillVcsNameCache();
        
        String key = display_name.toUpperCase().trim();
        Integer cacheId = vcsNameCache.get(key);
        if (cacheId != null) {
            return cacheId;
        }
        
        Integer idDeveloper = null;
        getCheckVcsDeveloperStmt();
        ResultSet rs = null;
        
        checkVcsDeveloperStmt.setString(1, key);
        
        rs = checkVcsDeveloperStmt.executeQuery();
        
        while (rs.next()) {
            idDeveloper = rs.getInt("alias_id");
        }
        
        rs.close();
        
        vcsNameCache.put(key, idDeveloper);
        
        if (idDeveloper == null) {
            return 0;
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
                            "FROM gros.commits c, gros.vcs_developer g" +
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
            
        catch (SQLException e) {
            printSQLExceptionDetails(e);
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
    
