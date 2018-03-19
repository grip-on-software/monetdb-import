/*
 * Copyright 2017-2018 ICTU
 * 
 * Importer of gathered software development metrics for MonetDB
 */
package dao;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import util.BaseDb;

/**
 * Class is created to manage the developer table of the database.
 * @author Enrique, Thomas
 */
public class DeveloperDb extends BaseDb implements AutoCloseable {
    private final String localDomain;
    private BatchedStatement insertDeveloperStmt = null;
    private PreparedStatement checkDeveloperStmt = null;
    
    private PreparedStatement insertVcsDeveloperStmt = null;
    private PreparedStatement checkVcsDeveloperStmt = null;
    private PreparedStatement searchVcsDeveloperStmt = null;
    private PreparedStatement linkVcsDeveloperStmt = null;
    
    private BatchedStatement insertProjectDeveloperStmt = null;
    private PreparedStatement checkProjectDeveloperStmt = null;
    private PreparedStatement checkProjectDeveloperIdStmt = null;
    
    private BatchedStatement insertLdapDeveloperStmt = null;
    private PreparedStatement checkLdapDeveloperStmt = null;
    private PreparedStatement linkLdapDeveloperStmt = null;
    
    private HashMap<String, Integer> vcsNameCache = null;
    private HashMap<Integer, HashMap<String, Integer>> ldapNameCache = null;
    
    public static class Developer {
        private final String name;
        private final String display_name;
        private final String email;
        
        public Developer(String display_name) {
            this(display_name, null);
        }
        
        public Developer(String display_name, String email) {
            this(null, display_name, email);
        }
        
        /**
         * Initialize a developer object.
         * @param name The shorthand name of the developer
         * @param display_name The long name of the developer as displayed in
         * certain user interfaces or in the version control system.
         * @param email The email address of the developer, may be null.
         */
        public Developer(String name, String display_name, String email) {
            this.name = (name == null ? display_name : name);
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

        /**
         * Check whether the email address of the developer uses the given domain name.
         * Developers that do not have an email address specified do not match.
         * @param localDomain The domain name to match against
         * @return Whether the email address ends with the domain name, immediately
         * preceded by an at sign (@).
         */
        private boolean matchEmailDomain(String localDomain) {
            if (this.email == null) {
                return false;
            }
            return this.email.endsWith("@" + localDomain);
        }
    }
    
    public DeveloperDb() {
        String sql = "insert into gros.developer (name,display_name,email,local_domain) values (?,?,?,?);";
        insertDeveloperStmt = new BatchedStatement(sql);

        sql = "insert into gros.project_developer (project_id, developer_id, name, display_name, email, encryption) values (?,?,?,?,?,?);";
        insertProjectDeveloperStmt = new BatchedStatement(sql);
        
        sql = "insert into gros.ldap_developer (project_id, name, display_name, email, jira_dev_id) values (?,?,?,?,?);";
        insertLdapDeveloperStmt = new BatchedStatement(sql);

        localDomain = getBundle().getString("email_domain");
    }
    
    /**
     * Inserts a developer in the developer table. 
     * @param dev Developer object with at least name and display name.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_developer(Developer dev) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertDeveloperStmt.getPreparedStatement();
        
        pstmt.setString(1, dev.getName());
        pstmt.setString(2, dev.getDisplayName());
        setString(pstmt, 3, dev.getEmail());
        pstmt.setBoolean(4, dev.matchEmailDomain(localDomain));
        
        // Insert immediately because we need to have the developer ID available
        // in the ImpDeveloper importer.
        pstmt.execute();
    }
    
    /**
     * Commits changes to the developer table and closes the connection.
     * @throws SQLException If a database access error occurs
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
        if (searchVcsDeveloperStmt != null) {
            searchVcsDeveloperStmt.close();
            searchVcsDeveloperStmt = null;
        }
        if (linkVcsDeveloperStmt != null) {
            linkVcsDeveloperStmt.close();
            linkVcsDeveloperStmt = null;
        }
        
        if (vcsNameCache != null) {
            vcsNameCache.clear();
            vcsNameCache = null;
        }

        insertProjectDeveloperStmt.execute();
        insertProjectDeveloperStmt.close();
        
        if (checkProjectDeveloperStmt != null) {
            checkProjectDeveloperStmt.close();
            checkProjectDeveloperStmt = null;
        }
        if (checkProjectDeveloperIdStmt != null) {
            checkProjectDeveloperIdStmt.close();
            checkProjectDeveloperIdStmt = null;
        }

        insertLdapDeveloperStmt.execute();
        insertLdapDeveloperStmt.close();
        
        if (checkLdapDeveloperStmt != null) {
            checkLdapDeveloperStmt.close();
            checkLdapDeveloperStmt = null;
        }
        if (linkLdapDeveloperStmt != null) {
            linkLdapDeveloperStmt.close();
            linkLdapDeveloperStmt = null;
        }
    }
    
    /**
     * Convert a display name of a developer to upper case.
     * This can be used for case-insensitive matching of unencrypted developer names.
     * @param name The display name
     * @return Uppercase variant of the name
     */
    private static String caseFold(String name) {
        if (name == null) {
            return null;
        }
        return name.toUpperCase().trim();
    }
    
    private void getCheckDeveloperStmt() throws SQLException, PropertyVetoException {
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
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public int check_developer(Developer dev) throws SQLException, PropertyVetoException {
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

    private void getInsertVcsDeveloperStmt() throws SQLException, PropertyVetoException {
        if (insertVcsDeveloperStmt == null) {
            Connection con = insertDeveloperStmt.getConnection();
            String sql = "insert into gros.vcs_developer (jira_dev_id, display_name, email, encryption) values (?,?,?,?);";
            insertVcsDeveloperStmt = con.prepareStatement(sql);
        } 
    }
    
    private void getCheckVcsDeveloperStmt() throws SQLException, PropertyVetoException {
        if (checkVcsDeveloperStmt == null) {
            Connection con = insertDeveloperStmt.getConnection();
            String sql = "SELECT alias_id FROM gros.vcs_developer WHERE ((encryption=? AND UPPER(display_name) = ?) OR (encryption=? AND display_name=?))";
            checkVcsDeveloperStmt = con.prepareStatement(sql);
        }
    }
    
    private void fillVcsNameCache() throws SQLException, PropertyVetoException {
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
    
    private void getSearchVcsDeveloperStmt() throws SQLException, PropertyVetoException {
        if (searchVcsDeveloperStmt == null) {
            Connection con = insertDeveloperStmt.getConnection();
            String sql = "SELECT display_name, email FROM gros.vcs_developer WHERE encryption = 0 AND display_name LIKE ?";
            searchVcsDeveloperStmt = con.prepareStatement(sql);
        }
    }
    
    private void getLinkVcsDeveloperStmt() throws SQLException, PropertyVetoException {
        if (linkVcsDeveloperStmt == null) {
            Connection con = insertDeveloperStmt.getConnection();
            String condition = "(encryption=? AND ((display_name IS NOT NULL AND display_name=?) OR (email IS NOT NULL AND email=?)))";
            String sql = "UPDATE gros.vcs_developer SET jira_dev_id=? WHERE (" + condition + " OR " + condition + ");";
            linkVcsDeveloperStmt = con.prepareStatement(sql);
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
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_vcs_developer(int dev_id, Developer dev, int encryption) throws SQLException, PropertyVetoException{
        getInsertVcsDeveloperStmt();
        
        insertVcsDeveloperStmt.setInt(1, dev_id);
        insertVcsDeveloperStmt.setString(2, dev.getDisplayName());
        setString(insertVcsDeveloperStmt, 3, dev.getEmail());
        insertVcsDeveloperStmt.setInt(4, encryption);
    
        // Insert immediately because we need to have the alias_id available
        // in update_vcs_developer.
        insertVcsDeveloperStmt.execute();
    }
   
    /**
     * Returns the Alias ID if the developer already exists in the VCS developer 
     * table of the database. Else returns 0.
     * @param display_name the complete name of the developer in the version control system.
     * @param encryption the encryption level of the provided display_name
     * of the developer (0=no encryption, 1=project encryption,
     * 2=global encrption, 3=project-then-global encryption).
     * @return the Developer ID if found, otherwise 0.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
    */
    public int check_vcs_developer(String display_name, int encryption) throws SQLException, PropertyVetoException {
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
    
    /**
     * Ensure that the provided developer exists in the VCS developers table.
     * This method checks if a developer with the provided developer's display name
     * already exists in the database (either with the provided encryption level
     * or higher levels of encryption). If the developer does not yet exist,
     * this method inserts the developer in the VCS developer table, linking it
     * with the global developer if a match can be made via the project developer
     * table (using the encryption level or higher levels of encryption).
     * @param project_id The project that the developer works on.
     * @param dev The developer to check and possibly insert
     * @param encryption The encryption level of the provided developer properties
     * @return The alias ID reference of the VCS developer, either an existing
     * reference or a newly inserted one
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public int update_vcs_developer(int project_id, Developer dev, int encryption) throws SQLException, PropertyVetoException {
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
    
    /**
     * Search for developers in the VCS developer table that match
     * the given display name pattern.
     * @param display_name An SQL LIKE pattern to search display names on.
     * @return A list of Developer objects for matching display names.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public List<Developer> search_vcs_developers(String display_name) throws SQLException, PropertyVetoException {
        getSearchVcsDeveloperStmt();
        
        searchVcsDeveloperStmt.setString(1, display_name);
        
        List<Developer> result = new ArrayList<>();
        try (ResultSet rs = searchVcsDeveloperStmt.executeQuery()) {
            while (rs.next()) {
                Developer matchDev = new Developer(rs.getString("display_name"), rs.getString("email"));
                result.add(matchDev);
            }
        }
        
        return result;
    }
    
    /**
     * Link VCS developers that have a certain display name or email with a JIRA
     * developer based on ID linking.
     * @param project_id The project ID in which the developer works on, for the
     * purpose of matching encrypted versions of the developer name.
     * @param jira_id The developer ID of the JIRA developer to link against.
     * @param dev The developer details to use when searching for the VCS
     * developer to link.
     * @return Whether the link was successful. A link is unsuccessful if the
     * provided JIRA ID is 0 or if there was no developer with the provided
     * display name or email (if available).
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public boolean link_vcs_developer(int project_id, int jira_id, Developer dev) throws SQLException, PropertyVetoException {
        getLinkVcsDeveloperStmt();
        
        if (jira_id == 0) {
            return false;
        }
        
        linkVcsDeveloperStmt.setInt(1, jira_id);
        
        linkVcsDeveloperStmt.setInt(2, SaltDb.Encryption.NONE);
        setString(linkVcsDeveloperStmt, 3, dev.getDisplayName());
        setString(linkVcsDeveloperStmt, 4, dev.getEmail());

        try (SaltDb saltDb = new SaltDb()) {
            SaltDb.SaltPair pair = saltDb.get_salt(project_id);
            linkVcsDeveloperStmt.setInt(5, project_id == 0 ? SaltDb.Encryption.GLOBAL : SaltDb.Encryption.PROJECT);
            setString(linkVcsDeveloperStmt, 6, saltDb.hash(dev.getDisplayName(), pair));
            setString(linkVcsDeveloperStmt, 7, saltDb.hash(dev.getEmail(), pair));
        }
        
        int rows = linkVcsDeveloperStmt.executeUpdate();
        return rows > 0;
    }
    
    private void getCheckProjectDeveloperStmt() throws SQLException, PropertyVetoException {
        if (checkProjectDeveloperStmt == null) {
            Connection con = insertDeveloperStmt.getConnection();
            String sql = "SELECT developer_id, name, email FROM gros.project_developer WHERE project_id = ? AND encryption = ? AND (name = ? OR display_name = ? OR (email IS NOT NULL AND email = ?))";
            checkProjectDeveloperStmt = con.prepareStatement(sql);
        }
    }
    
    private void getCheckProjectDeveloperIdStmt() throws SQLException, PropertyVetoException {
        if (checkProjectDeveloperIdStmt == null) {
            Connection con = insertDeveloperStmt.getConnection();
            String sql = "SELECT developer_id FROM gros.project_developer WHERE project_id = ? AND developer_id = ?";
            checkProjectDeveloperIdStmt = con.prepareStatement(sql);
        }
    }
    
    /**
     * Inserts a developer in the project developer table of the database.
     * @param project_id the project the developer works on.
     * @param dev_id the corresponding developer id in Jira 
     * @param dev The project developer.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_project_developer(int project_id, int dev_id, Developer dev) throws SQLException, PropertyVetoException{
        getCheckProjectDeveloperIdStmt();
        
        // Check if the project developer already exists
        checkProjectDeveloperIdStmt.setInt(1, project_id);
        checkProjectDeveloperIdStmt.setInt(2, dev_id);
        try (ResultSet rs = checkProjectDeveloperIdStmt.executeQuery()) {
            if (rs.next()) {
                return;
            }
        }
        
        PreparedStatement pstmt = insertProjectDeveloperStmt.getPreparedStatement();
        
        try (SaltDb saltDb = new SaltDb()) {
            SaltDb.SaltPair pair = saltDb.get_salt(project_id);
            
            pstmt.setInt(1, project_id);
            pstmt.setInt(2, dev_id);
            pstmt.setString(3, saltDb.hash(dev.getName(), pair));
            setString(pstmt, 4, saltDb.hash(dev.getDisplayName(), pair));
            setString(pstmt, 5, saltDb.hash(dev.getEmail(), pair));
            pstmt.setInt(6, SaltDb.Encryption.PROJECT);
    
            // Execute immediately to avoid primary key constraint violations
            pstmt.execute();
        }
    }
    
    /**
     * Check if a developer is registered in the project developer table, either
     * with their display name or email address. This is used for matching VCS
     * developers with their global JIRA developer counterpart.
     * @param project_id the project the developer works on, or 0 to use the
     * global JIRA developer table instead.
     * @param dev The project developer.
     * @param encryption the encryption level of the developer's display name and
     * email address (0=no encryption, 1=project encryption,
     * 2=global encrption, 3=project-then-global encryption).
     * @return The ID in the developer table, or 0 if the developer was not found.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public int check_project_developer(int project_id, Developer dev, int encryption) throws SQLException, PropertyVetoException {
        if (project_id == 0) {
            return check_developer(dev);
        }
        
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
                encryption = SaltDb.Encryption.PROJECT;
            }
        }
        
        checkProjectDeveloperStmt.setInt(1, project_id);
        checkProjectDeveloperStmt.setInt(2, encryption);
        setString(checkProjectDeveloperStmt, 3, name);
        setString(checkProjectDeveloperStmt, 4, display_name);
        setString(checkProjectDeveloperStmt, 5, email);

        int idDeveloper = 0;
        try (ResultSet rs = checkProjectDeveloperStmt.executeQuery()) {
            while (rs.next()) {
                idDeveloper = rs.getInt("developer_id");
            }
        }
        
        return idDeveloper;
    }

    private void getCheckLdapDeveloperStmt() throws SQLException, PropertyVetoException {
        if (checkLdapDeveloperStmt == null) {
            Connection con = insertDeveloperStmt.getConnection();
            String sql = "SELECT jira_dev_id FROM gros.ldap_developer WHERE ((encryption=? AND name=?) OR (encryption=? AND name=?))";
            checkLdapDeveloperStmt = con.prepareStatement(sql);
        }
    }
    
    private void getLinkLdapDeveloperStmt() throws SQLException, PropertyVetoException {
        if (linkLdapDeveloperStmt == null) {
            Connection con = insertDeveloperStmt.getConnection();
            String condition = "(encryption=? AND ((display_name IS NOT NULL AND display_name=?) OR (email IS NOT NULL AND email=?)))";
            String sql = "UPDATE gros.ldap_developer SET jira_dev_id=? WHERE (" + condition + " OR " + condition + ");";
            linkLdapDeveloperStmt = con.prepareStatement(sql);
        }
    }

    /**
     * Insert a developer in the LDAP developer table of the database. In case developer
     * id is not set, the developer id from Jira will be 0.
     * @param project_id The project identifier of the project in which the developer
     * has an LDAP account
     * @param dev_id The corresponding developer id in Jira, or 0.
     * @param dev The developer object, with at least name and display name.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_ldap_developer(int project_id, int dev_id, Developer dev) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertLdapDeveloperStmt.getPreparedStatement();
        pstmt.setInt(1, project_id);
        pstmt.setString(2, dev.getName());
        pstmt.setString(3, dev.getDisplayName());
        setString(pstmt, 4, dev.getEmail());
        pstmt.setInt(5, dev_id);

        insertLdapDeveloperStmt.batch();
        
        insertLdapNameCache(project_id, dev.getName(), dev_id);
    }
    
    private void insertLdapNameCache(Integer project_id, String name, Integer jira_id) {
        if (ldapNameCache == null) {
            return;
        }
        
        HashMap<String, Integer> projectCache = ldapNameCache.get(project_id);
        if (projectCache == null) {
            projectCache = new HashMap<>();
            ldapNameCache.put(project_id, projectCache);
        }
        projectCache.put(name, jira_id);        
    }
    
    private void fillLdapNameCache() throws SQLException, PropertyVetoException {
        if (ldapNameCache != null) {
            return;
        }
        ldapNameCache = new HashMap<>();
        
        Connection con = insertDeveloperStmt.getConnection();
        String sql = "SELECT project_id, name, jira_dev_id FROM gros.ldap_developer";
        try (
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(sql)
        ) {
            while (rs.next()) {
                Integer project_id = rs.getInt("project_id");
                String name = rs.getString("name");
                Integer id = rs.getInt("jira_dev_id");
                insertLdapNameCache(project_id, name, id);
            }
        }
    }
    
    /**
     * Retrieve the JIRA developer ID of an LDAP developer.
     * @param project_id Project identifier of the project in which the developer
     * has an LDAP account.
     * @param dev The developer object, with at least the name of the developer.
     * @param encryption The encryption level of the provided developer properties.
     * @return The Jira developer identifier: 0 if the LDAP developer exists but
     * is not linked to a Jira developer, -1 if the LDAP developer exists but is
     * explicitly not linked due to it being a system account, null if the LDAP
     * developer does not exist, or any other value indicating the link from an
     * existing LDAP developer.
     * @throws SQLException
     * @throws PropertyVetoException 
     */
    public Integer check_ldap_developer(int project_id, Developer dev, int encryption) throws SQLException, PropertyVetoException {
        fillLdapNameCache();
        
        HashMap<String, Integer> projectCache = ldapNameCache.get(project_id);
        String plain_name;
        String encrypted_name;
        if (encryption == SaltDb.Encryption.NONE) {
            plain_name = dev.getName();
            try (SaltDb saltDb = new SaltDb()) {
                SaltDb.SaltPair pair = saltDb.get_salt(project_id);
                encrypted_name = saltDb.hash(dev.getName(), pair);
                encryption = SaltDb.Encryption.PROJECT;
            }
        }
        else {
            // Cannot decrypt the display name at this point
            plain_name = dev.getName();
            encrypted_name = dev.getName();
        }
        if (projectCache != null) {
            Integer cacheId = projectCache.get(plain_name);
            if (cacheId != null) {
                return cacheId;
            }
            cacheId = projectCache.get(encrypted_name);
            if (cacheId != null) {
                return cacheId;
            }
        }
        
        Integer idDeveloper = null;
        getCheckLdapDeveloperStmt();
        
        checkLdapDeveloperStmt.setInt(1, SaltDb.Encryption.NONE);
        checkLdapDeveloperStmt.setString(2, plain_name);
        checkLdapDeveloperStmt.setInt(3, encryption);
        checkLdapDeveloperStmt.setString(4, encrypted_name);
        
        try (ResultSet rs = checkLdapDeveloperStmt.executeQuery()) {
            while (rs.next()) {
                idDeveloper = rs.getInt("jira_dev_id");
            }
        }
        
        if (idDeveloper != null) {
            insertLdapNameCache(project_id, plain_name, idDeveloper);
            insertLdapNameCache(project_id, encrypted_name, idDeveloper);
        }
        
        return idDeveloper;
    }

    /**
     * Link an LDAP developers that have a certain display name or email with a JIRA
     * developer based on ID linking.
     * @param project_id The project ID in which the LDAP developer works on.
     * @param jira_id The developer ID of the JIRA developer to link against.
     * @param dev The developer details to use when searching for the LDAP
     * developer to link.
     * @return Whether the link was successful. A link is unsuccessful if the
     * provided JIRA ID is 0 or if there was no developer with the provided
     * display name or email (if available).
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public boolean link_ldap_developer(int project_id, int jira_id, Developer dev) throws SQLException, PropertyVetoException {
        getLinkLdapDeveloperStmt();
        
        if (project_id == 0 || jira_id == 0) {
            return false;
        }
        
        linkLdapDeveloperStmt.setInt(1, jira_id);
        
        linkLdapDeveloperStmt.setInt(2, SaltDb.Encryption.NONE);
        setString(linkLdapDeveloperStmt, 3, dev.getDisplayName());
        setString(linkLdapDeveloperStmt, 4, dev.getEmail());

        try (SaltDb saltDb = new SaltDb()) {
            SaltDb.SaltPair pair = saltDb.get_salt(project_id);
            linkLdapDeveloperStmt.setInt(5, SaltDb.Encryption.PROJECT);
            setString(linkLdapDeveloperStmt, 6, saltDb.hash(dev.getDisplayName(), pair));
            setString(linkLdapDeveloperStmt, 7, saltDb.hash(dev.getEmail(), pair));
        }
        
        int rows = linkLdapDeveloperStmt.executeUpdate();
        return rows > 0;
    }
}
    
