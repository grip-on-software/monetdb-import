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
    BatchedStatement insertDeveloperStmt = null;
    PreparedStatement checkDeveloperStmt = null;
    
    PreparedStatement insertVcsDeveloperStmt = null;
    PreparedStatement checkVcsDeveloperStmt = null;
    
    PreparedStatement insertProjectDeveloperStmt = null;
    PreparedStatement checkProjectDeveloperStmt = null;
    
    HashMap<String, Integer> vcsNameCache = null;
    
    public DeveloperDb() {
        String sql = "insert into gros.developer (name,display_name,email) values (?,?,?);";
        insertDeveloperStmt = new BatchedStatement(sql);
    }
    
    /**
     * Inserts developer in the developer table. 
     * @param name the username in Jira
     * @param display_name The complete name of the user
     * @param email The email address of the user, or null.
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.beans.PropertyVetoException
     */
    public void insert_developer(String name, String display_name, String email) throws SQLException, IOException, PropertyVetoException{
        PreparedStatement pstmt = insertDeveloperStmt.getPreparedStatement();
        
        pstmt.setString(1, name);
        pstmt.setString(2, display_name);
        setString(pstmt, 3, email);
        
        // Insert immediately because we need to have the row available
        pstmt.execute();
    }
    
    /**
     * Commits changes to the developer table and closes the connection.
     * @throws java.sql.SQLException
     */
    @Override
    public void close() throws SQLException {
        insertDeveloperStmt.execute();
        insertDeveloperStmt.close();
        
        if (checkDeveloperStmt != null) {
            checkDeveloperStmt.close();
        }
        if (insertVcsDeveloperStmt != null) {
            insertVcsDeveloperStmt.close();
        }
        if (checkVcsDeveloperStmt != null) {
            checkVcsDeveloperStmt.close();
        }
        if (insertProjectDeveloperStmt != null) {
            insertProjectDeveloperStmt.execute();
            insertProjectDeveloperStmt.close();
        }
        
        if (vcsNameCache != null) {
            vcsNameCache.clear();
            vcsNameCache = null;
        }
    }
    
    private void getCheckDeveloperStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkDeveloperStmt == null) {
            Connection con = insertDeveloperStmt.getConnection();
            String sql = "SELECT id FROM gros.developer WHERE UPPER(name) = ? OR UPPER(display_name) = ? OR UPPER(email) = ?";
            checkDeveloperStmt = con.prepareStatement(sql);
        }
    }
    
    /**
     * Returns the developer ID if the developer already exists in the developer 
     * table of the database. Else returns 0.
     * @param name the short alias of the developer in Jira.
     * @param display_name the complete name of the developer in Jira.
     * @param email The email address of the developer, or null.
     * @return the Developer ID if found, otherwise 0.
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.beans.PropertyVetoException
     */
    public int check_developer(String name, String display_name, String email) throws SQLException, IOException, PropertyVetoException {
        int idDeveloper = 0;
        getCheckDeveloperStmt();
        
        checkDeveloperStmt.setString(1, name.toUpperCase().trim());
        checkDeveloperStmt.setString(2, display_name.toUpperCase().trim());
        if (email == null) {
            email = name;
        }
        checkDeveloperStmt.setString(3, email.toUpperCase().trim());
        try (ResultSet rs = checkDeveloperStmt.executeQuery()) {
            while (rs.next()) {
                idDeveloper = rs.getInt("id");
            }
        }
        
        return idDeveloper;
    } 
    
    private void getInsertVcsDeveloperStmt() throws SQLException, IOException, PropertyVetoException {
        if (insertVcsDeveloperStmt == null) {
            Connection con = insertDeveloperStmt.getConnection();
            String sql = "insert into gros.vcs_developer (jira_dev_id, display_name, email, encrypted) values (?,?,?,?);";
            insertVcsDeveloperStmt = con.prepareStatement(sql);
        } 
    }
    
    /**
     * Inserts developers in the VCS developer table of the database. In case developer
     * id is not set, the developer id from Jira will be 0. The alias id's are initialized
     * incremental by the database.
     * @param dev_id the corresponding developer id in Jira 
     * @param display_name the full name of the user in the version control system.
     * @param email The email address of the developer, or null.
     * @param encrypted whether the name and email address of the developer are already encrypted.
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.beans.PropertyVetoException
     */
    public void insert_vcs_developer(int dev_id, String display_name, String email, boolean encrypted) throws SQLException, IOException, PropertyVetoException{
        getInsertVcsDeveloperStmt();
        
        insertVcsDeveloperStmt.setInt(1, dev_id);
        insertVcsDeveloperStmt.setString(2, display_name);
        setString(insertVcsDeveloperStmt, 3, email);
        insertVcsDeveloperStmt.setBoolean(4, encrypted);
    
        insertVcsDeveloperStmt.execute();
    }
   
    private void getCheckVcsDeveloperStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkVcsDeveloperStmt == null) {
            Connection con = insertDeveloperStmt.getConnection();
            String sql = "SELECT alias_id FROM gros.vcs_developer WHERE UPPER(display_name) = ?";
            checkVcsDeveloperStmt = con.prepareStatement(sql);
        }
    }
    
    private void fillVcsNameCache() throws SQLException, IOException, PropertyVetoException {
        if (vcsNameCache != null) {
            return;
        }
        vcsNameCache = new HashMap<>();
        
        Connection con = insertDeveloperStmt.getConnection();
        String sql = "SELECT UPPER(display_name), alias_id FROM gros.vcs_developer";
        try (
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(sql)
        ) {
            while(rs.next()) {
                String key = rs.getString(1);
                Integer id = Integer.parseInt(rs.getString(2));
                vcsNameCache.put(key, id);
            }
        }
    }
    
    /**
     * Returns the Alias ID if the developer already exists in the VCS developer 
     * table of the database. Else returns 0.
     * @param display_name the complete name of the developer in the version control system.
     * @return the Developer ID if found, otherwise 0.
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.beans.PropertyVetoException
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
        
        checkVcsDeveloperStmt.setString(1, key);
        
        try (ResultSet rs = checkVcsDeveloperStmt.executeQuery()) {
            while (rs.next()) {
                idDeveloper = rs.getInt("alias_id");
            }
        }
        
        vcsNameCache.put(key, idDeveloper);
        
        if (idDeveloper == null) {
            return 0;
        }
        
        return idDeveloper;
    }
    
    public int update_vcs_developer(int project_id, String display_name, String email, boolean encrypted) throws SQLException, IOException, PropertyVetoException {
        int vcs_developer_id = check_vcs_developer(display_name);
        if (vcs_developer_id == 0) {
            // If the VCS developer does not exist, create a new VCS developer
            // Check if JIRA developer exists with the same (short) name or email
            // This may return 0, which indicates that it should be linked (manually) later.
            int jira_developer_id = check_project_developer(project_id, display_name, email, encrypted);
            insert_vcs_developer(jira_developer_id, display_name, email, encrypted);
            // Retrieve new VCS developer ID
            vcs_developer_id = check_vcs_developer(display_name);
        }

        return vcs_developer_id;
    }

    private void getInsertProjectDeveloperStmt() throws SQLException, IOException, PropertyVetoException {
        if (insertProjectDeveloperStmt == null) {
            Connection con = insertDeveloperStmt.getConnection();
            String sql = "insert into gros.project_developer (project_id, developer_id, name, display_name, email, encrypted) values (?,?,?,?,?,?);";
            insertProjectDeveloperStmt = con.prepareStatement(sql);
        } 
    }
    
    private void getCheckProjectDeveloperStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkProjectDeveloperStmt == null) {
            Connection con = insertDeveloperStmt.getConnection();
            String sql = "SELECT developer_id, name, email FROM gros.project_developer WHERE project_id = ? AND (display_name = ? OR (email IS NOT NULL AND email = ?))";
            checkProjectDeveloperStmt = con.prepareStatement(sql);
        }
    }
    
    /**
     * Inserts a developer in the project developer table of the database.
     * @param project_id the project the developer works on.
     * @param dev_id the corresponding developer id in Jira 
     * @param name 
     * @param display_name the full name of the user in the version control system.
     * @param email The email address of the developer, or null.
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.beans.PropertyVetoException
     */
    public void insert_project_developer(int project_id, int dev_id, String name, String display_name, String email) throws SQLException, IOException, PropertyVetoException{
        getInsertVcsDeveloperStmt();
        
        try (SaltDb saltDb = new SaltDb()) {
            SaltDb.SaltPair pair = saltDb.get_salt(project_id);
            
            insertProjectDeveloperStmt.setInt(1, project_id);
            insertProjectDeveloperStmt.setInt(2, dev_id);
            insertProjectDeveloperStmt.setString(3, saltDb.hash(name, pair));
            setString(insertProjectDeveloperStmt, 4, saltDb.hash(display_name, pair));
            setString(insertProjectDeveloperStmt, 5, saltDb.hash(email, pair));
            insertProjectDeveloperStmt.setBoolean(6, true);
    
            insertProjectDeveloperStmt.execute();
        }
    }
    
    /**
     * Check if a developer is registered in the project developer table, either
     * with their display name or email address. This is used for matching VCS
     * developers with their global JIRA developer counterpart.
     * @param project_id the project the developer works on.
     * @param display_name the full name of the user in the version control system.
     * @param email the email address of the developer, or null.
     * @param encrypted whether the full name and email address of the developer is already encrypted.
     * @return The ID in the developer table, or 0 if the developer was not found.
     * @throws SQLException
     * @throws IOException
     * @throws PropertyVetoException 
     */
    public int check_project_developer(int project_id, String display_name, String email, boolean encrypted) throws SQLException, IOException, PropertyVetoException {
        getCheckProjectDeveloperStmt();
        
        if (!encrypted) {
            try (SaltDb saltDb = new SaltDb()) {
                SaltDb.SaltPair pair = saltDb.get_salt(project_id);
                display_name = saltDb.hash(display_name, pair);
                email = saltDb.hash(email, pair);
            }
        }
        
        checkProjectDeveloperStmt.setInt(1, project_id);
        checkProjectDeveloperStmt.setString(2, display_name);
        checkProjectDeveloperStmt.setString(3, email);

        int idDeveloper = 0;
        try (ResultSet rs = checkVcsDeveloperStmt.executeQuery()) {
            while (rs.next()) {
                idDeveloper = rs.getInt("alias_id");
            }
        }
        
        return idDeveloper;
    }

}
    
