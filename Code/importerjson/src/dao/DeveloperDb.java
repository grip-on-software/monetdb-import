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
    
    public static class Developer {
        private final String name;
        private final String display_name;
        private final String email;
        
        public Developer(String display_name) {
            this(display_name, null);
        }
        
        public Developer(String display_name, String email) {
            this(display_name, display_name, email);
        }
        
        /**
         * Initialize a developer object.
         * @param name The shorthand name of the developer
         * @param display_name The long name of the developer as displayed in
         * certain user interfaces or in the version control system.
         * @param email The email address of the developer, may be null.
         */
        public Developer(String name, String display_name, String email) {
            this.name = name;
            this.display_name = display_name;
            this.email = email;
        }
        
        public String getName() {
            return this.name;
        }
        
        public String getDisplayName() {
            return this.display_name;
        }
        
        public String getEmail() {
            return this.email;
        }
    }
    
    public DeveloperDb() {
        String sql = "insert into gros.developer (name,display_name,email) values (?,?,?);";
        insertDeveloperStmt = new BatchedStatement(sql);
    }
    
    /**
     * Inserts developer in the developer table. 
     * @param dev Developer object with at least name and display name.
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.beans.PropertyVetoException
     */
    public void insert_developer(Developer dev) throws SQLException, IOException, PropertyVetoException {
        PreparedStatement pstmt = insertDeveloperStmt.getPreparedStatement();
        
        pstmt.setString(1, dev.getName());
        pstmt.setString(2, dev.getDisplayName());
        setString(pstmt, 3, dev.getEmail());
        
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
            checkDeveloperStmt = null;
        }
        if (insertVcsDeveloperStmt != null) {
            insertVcsDeveloperStmt.close();
            insertVcsDeveloperStmt = null;
        }
        if (checkVcsDeveloperStmt != null) {
            checkVcsDeveloperStmt.close();
            checkVcsDeveloperStmt = null;
        }
        if (insertProjectDeveloperStmt != null) {
            insertProjectDeveloperStmt.close();
            insertProjectDeveloperStmt = null;
        }
        
        if (vcsNameCache != null) {
            vcsNameCache.clear();
            vcsNameCache = null;
        }
    }
    
    private static String caseFold(String name) {
        if (name == null) {
            return null;
        }
        return name.toUpperCase().trim();
    }
    
    private void getCheckDeveloperStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkDeveloperStmt == null) {
            Connection con = insertDeveloperStmt.getConnection();
            String sql = "SELECT id FROM gros.developer WHERE ((encryption=? AND (UPPER(name) = ? OR UPPER(display_name) = ? OR UPPER(email) = ?)) OR (encryption=? AND (name=? OR display_name=? OR email=?)))";
            checkDeveloperStmt = con.prepareStatement(sql);
        }
    }
    
    /**
     * Returns the developer ID if the developer already exists in the developer 
     * table of the database. Else returns 0.
     * @param dev The developer object, with at least name and display name.
     * @return the Developer ID if found, otherwise 0.
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.beans.PropertyVetoException
     */
    public int check_developer(Developer dev) throws SQLException, IOException, PropertyVetoException {
        int idDeveloper = 0;
        getCheckDeveloperStmt();
        
        String name = dev.getName();
        String display_name = dev.getDisplayName();
        String email = dev.getEmail();
        if (email == null) {
            email = name;
        }
        
        checkDeveloperStmt.setInt(1, SaltDb.Encryption.NONE);
        setString(checkDeveloperStmt, 2, caseFold(name));
        setString(checkDeveloperStmt, 3, caseFold(display_name));
        setString(checkDeveloperStmt, 4, caseFold(email));
        
        checkDeveloperStmt.setInt(5, SaltDb.Encryption.GLOBAL);
        try (SaltDb saltDb = new SaltDb()) {
            SaltDb.SaltPair pair = saltDb.get_salt(0);
            setString(checkDeveloperStmt, 6, saltDb.hash(name, pair));
            setString(checkDeveloperStmt, 7, saltDb.hash(display_name, pair));
            setString(checkDeveloperStmt, 8, saltDb.hash(email, pair));
        }
        
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
            String sql = "insert into gros.vcs_developer (jira_dev_id, display_name, email, encryption) values (?,?,?,?);";
            insertVcsDeveloperStmt = con.prepareStatement(sql);
        } 
    }
    
    /**
     * Inserts developers in the VCS developer table of the database. In case developer
     * id is not set, the developer id from Jira will be 0. The alias id's are initialized
     * incremental by the database.
     * @param dev_id the corresponding developer id in Jira 
     * @param dev The developer object, with at least display name.
     * Developer shorthand name is ignored.
     * @param encryption the encryption level of the provided display_name and
     * email address of the developer (0=no encryption, 1=project encryption,
     * 2=global encrption, 3=project-then-global encryption).
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.beans.PropertyVetoException
     */
    public void insert_vcs_developer(int dev_id, Developer dev, int encryption) throws SQLException, IOException, PropertyVetoException{
        getInsertVcsDeveloperStmt();
        
        insertVcsDeveloperStmt.setInt(1, dev_id);
        insertVcsDeveloperStmt.setString(2, dev.getDisplayName());
        setString(insertVcsDeveloperStmt, 3, dev.getEmail());
        insertVcsDeveloperStmt.setInt(4, encryption);
    
        insertVcsDeveloperStmt.execute();
    }
   
    private void getCheckVcsDeveloperStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkVcsDeveloperStmt == null) {
            Connection con = insertDeveloperStmt.getConnection();
            String sql = "SELECT alias_id FROM gros.vcs_developer WHERE ((encryption=? AND UPPER(display_name) = ?) OR (encryption=? AND display_name=?))";
            checkVcsDeveloperStmt = con.prepareStatement(sql);
        }
    }
    
    private void fillVcsNameCache() throws SQLException, IOException, PropertyVetoException {
        if (vcsNameCache != null) {
            return;
        }
        vcsNameCache = new HashMap<>();
        
        Connection con = insertDeveloperStmt.getConnection();
        String sql = "SELECT display_name, alias_id, encryption FROM gros.vcs_developer";
        try (
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(sql)
        ) {
            while (rs.next()) {
                String display_name = rs.getString("display_name");
                Integer id = rs.getInt("alias_id");
                int encryption = rs.getInt("encryption");
                if (encryption == SaltDb.Encryption.NONE) {
                    display_name = caseFold(display_name);
                }
                vcsNameCache.put(display_name, id);
            }
        }
    }
    
    /**
     * Returns the Alias ID if the developer already exists in the VCS developer 
     * table of the database. Else returns 0.
     * @param display_name the complete name of the developer in the version control system.
     * @param encryption the encryption level of the provided display_name
     * of the developer (0=no encryption, 1=project encryption,
     * 2=global encrption, 3=project-then-global encryption).
     * @return the Developer ID if found, otherwise 0.
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.beans.PropertyVetoException
    */
    public int check_vcs_developer(String display_name, int encryption) throws SQLException, IOException, PropertyVetoException {
        fillVcsNameCache();
        
        String plain_name;
        String encrypted_name;
        if (encryption == SaltDb.Encryption.NONE) {
            plain_name = caseFold(display_name);
            try (SaltDb saltDb = new SaltDb()) {
                SaltDb.SaltPair pair = saltDb.get_salt(0);
                encrypted_name = saltDb.hash(display_name, pair);
                encryption = SaltDb.Encryption.GLOBAL;
            }
        }
        else {
            // Cannot decrypt the display name at this point
            plain_name = display_name;
            encrypted_name = display_name;
        }
        
        Integer cacheId = vcsNameCache.get(plain_name);
        if (cacheId != null) {
            return cacheId;
        }
        cacheId = vcsNameCache.get(encrypted_name);
        if (cacheId != null) {
            return cacheId;
        }
        
        Integer idDeveloper = null;
        getCheckVcsDeveloperStmt();
        
        checkVcsDeveloperStmt.setInt(1, SaltDb.Encryption.NONE);
        checkVcsDeveloperStmt.setString(2, plain_name);
        checkVcsDeveloperStmt.setInt(3, encryption);
        checkVcsDeveloperStmt.setString(4, encrypted_name);
        
        try (ResultSet rs = checkVcsDeveloperStmt.executeQuery()) {
            while (rs.next()) {
                idDeveloper = rs.getInt("alias_id");
            }
        }
        
        vcsNameCache.put(plain_name, idDeveloper);
        vcsNameCache.put(encrypted_name, idDeveloper);
        
        if (idDeveloper == null) {
            return 0;
        }
        
        return idDeveloper;
    }
    
    public int update_vcs_developer(int project_id, Developer dev, int encryption) throws SQLException, IOException, PropertyVetoException {
        String display_name = dev.getDisplayName();
        int vcs_developer_id = check_vcs_developer(display_name, encryption);
        if (vcs_developer_id == 0) {
            // If the VCS developer does not exist, create a new VCS developer
            // Check if JIRA developer exists with the same (short) name or email
            // This may return 0, which indicates that it should be linked (manually) later.
            int jira_developer_id = check_project_developer(project_id, dev, encryption);
            insert_vcs_developer(jira_developer_id, dev, encryption);
            // Retrieve new VCS developer ID
            vcs_developer_id = check_vcs_developer(display_name, encryption);
        }

        return vcs_developer_id;
    }

    private void getInsertProjectDeveloperStmt() throws SQLException, IOException, PropertyVetoException {
        if (insertProjectDeveloperStmt == null) {
            Connection con = insertDeveloperStmt.getConnection();
            String sql = "insert into gros.project_developer (project_id, developer_id, name, display_name, email, encryption) values (?,?,?,?,?,?);";
            insertProjectDeveloperStmt = con.prepareStatement(sql);
        } 
    }
    
    private void getCheckProjectDeveloperStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkProjectDeveloperStmt == null) {
            Connection con = insertDeveloperStmt.getConnection();
            String sql = "SELECT developer_id, name, email FROM gros.project_developer WHERE project_id = ? AND encryption = ? AND (name = ? OR display_name = ? OR (email IS NOT NULL AND email = ?))";
            checkProjectDeveloperStmt = con.prepareStatement(sql);
        }
    }
    
    /**
     * Inserts a developer in the project developer table of the database.
     * @param project_id the project the developer works on.
     * @param dev_id the corresponding developer id in Jira 
     * @param dev The project developer.
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.beans.PropertyVetoException
     */
    public void insert_project_developer(int project_id, int dev_id, Developer dev) throws SQLException, IOException, PropertyVetoException{
        getInsertProjectDeveloperStmt();
        
        try (SaltDb saltDb = new SaltDb()) {
            SaltDb.SaltPair pair = saltDb.get_salt(project_id);
            
            insertProjectDeveloperStmt.setInt(1, project_id);
            insertProjectDeveloperStmt.setInt(2, dev_id);
            insertProjectDeveloperStmt.setString(3, saltDb.hash(dev.getName(), pair));
            setString(insertProjectDeveloperStmt, 4, saltDb.hash(dev.getDisplayName(), pair));
            setString(insertProjectDeveloperStmt, 5, saltDb.hash(dev.getEmail(), pair));
            insertProjectDeveloperStmt.setInt(6, SaltDb.Encryption.PROJECT);
    
            insertProjectDeveloperStmt.execute();
        }
    }
    
    /**
     * Check if a developer is registered in the project developer table, either
     * with their display name or email address. This is used for matching VCS
     * developers with their global JIRA developer counterpart.
     * @param project_id the project the developer works on.
     * @param dev The project developer.
     * @param encryption the encryption level of the developer's display name and
     * email address (0=no encryption, 1=project encryption,
     * 2=global encrption, 3=project-then-global encryption).
     * @return The ID in the developer table, or 0 if the developer was not found.
     * @throws SQLException
     * @throws IOException
     * @throws PropertyVetoException 
     */
    public int check_project_developer(int project_id, Developer dev, int encryption) throws SQLException, IOException, PropertyVetoException {
        getCheckProjectDeveloperStmt();
        
        String name = dev.getName();
        String display_name = dev.getDisplayName();
        String email = dev.getEmail();
        if (encryption == SaltDb.Encryption.NONE) {
            try (SaltDb saltDb = new SaltDb()) {
                SaltDb.SaltPair pair = saltDb.get_salt(project_id);
                name = saltDb.hash(name, pair);
                display_name = saltDb.hash(display_name, pair);
                email = saltDb.hash(email, pair);
            }
        }
        
        checkProjectDeveloperStmt.setInt(1, project_id);
        checkProjectDeveloperStmt.setInt(2, encryption);
        setString(checkProjectDeveloperStmt, 3, name);
        setString(checkProjectDeveloperStmt, 4, display_name);
        setString(checkProjectDeveloperStmt, 5, email);

        int idDeveloper = 0;
        try (ResultSet rs = checkVcsDeveloperStmt.executeQuery()) {
            while (rs.next()) {
                idDeveloper = rs.getInt("alias_id");
            }
        }
        
        return idDeveloper;
    }

}
    
