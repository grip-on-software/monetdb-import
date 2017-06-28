/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import util.BaseDb;

/**
 * Class is created to manage the repository table of the database.
 * @author Enrique, Thomas
 */
public class RepositoryDb extends BaseDb implements AutoCloseable {
    private BatchedStatement insertRepoStmt = null;
    private PreparedStatement checkRepoStmt = null;
    private HashMap<Integer, HashMap<String, Integer>> nameCache = null;
    private BatchedStatement insertGitLabRepoStmt = null;
    private PreparedStatement checkGitLabRepoStmt = null;
    private BatchedStatement updateGitLabRepoStmt = null;

    public enum CheckResult {
        MISSING, DIFFERS, EXISTS
    };
    
    public RepositoryDb() {
        String sql = "insert into gros.repo (repo_name,project_id) values (?,?);";
        insertRepoStmt = new BatchedStatement(sql);
        
        sql = "insert into gros.gitlab_repo (repo_id,gitlab_id,description,create_date,archived,has_avatar,star_count) values (?,?,?,?,?,?,?);";
        insertGitLabRepoStmt = new BatchedStatement(sql);
        
        sql = "update gros.gitlab_repo set description=?, create_date=?, archived=?, has_avatar=?, star_count=? where repo_id=? AND gitlab_id=?;";
        updateGitLabRepoStmt = new BatchedStatement(sql);
    }
    
    @Override
    public void close() throws SQLException {
        // All insert statements are executed immediately, so no need to do so now
        insertRepoStmt.close();
        
        if (checkRepoStmt != null) {
            checkRepoStmt.close();
            checkRepoStmt = null;
        }
        
        if (nameCache != null) {
            nameCache.clear();
            nameCache = null;
        }
        
        insertGitLabRepoStmt.execute();
        insertGitLabRepoStmt.close();
        
        updateGitLabRepoStmt.execute();
        updateGitLabRepoStmt.close();
        
        if (checkGitLabRepoStmt != null) {
            checkGitLabRepoStmt.close();
            checkGitLabRepoStmt = null;
        }
    }
    
    /**
     * Inserts repository in the repo table. 
     * @param name The complete name of the repository.
     * @param project_id The project in which the repository is used.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_repo(String name, int project_id) throws SQLException, PropertyVetoException{
        PreparedStatement pstmt = insertRepoStmt.getPreparedStatement();
        
        pstmt.setString(1, name);
        pstmt.setInt(2, project_id);
        
        // Insert immediately because we need to have the row available
        pstmt.execute();
    }
    
    private void getCheckRepoStmt() throws SQLException, PropertyVetoException {
        if (checkRepoStmt == null) {
            Connection con = insertRepoStmt.getConnection();
            String sql = "SELECT id FROM gros.repo WHERE UPPER(repo_name) = ? AND project_id = ?";
            checkRepoStmt = con.prepareStatement(sql);
        }
    }
    
    private void insertCache(String key, Integer project_id, Integer id) {
        if (!nameCache.containsKey(project_id)) {
            nameCache.put(project_id, new HashMap<String, Integer>());
        }
        nameCache.get(project_id).put(key, id);
    }
    
    private void fillNameCache() throws SQLException, PropertyVetoException {
        if (nameCache != null) {
            return;
        }
        nameCache = new HashMap<>();
        
        Connection con = insertRepoStmt.getConnection();
        String sql = "SELECT UPPER(repo_name) AS repo_key, project_id, id FROM gros.repo";
        try (Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {
            while(rs.next()) {
                String key = rs.getString("repo_key");
                Integer project_id = rs.getInt("project_id");
                Integer id = rs.getInt("id");
                insertCache(key, project_id, id);
            }
        }
    }
    
    /**
     * Returns the repository ID if the repository already exists in the repo 
     * table of the database for the given project ID. Otherwise, returns 0.
     * @param name The complete name of the repository.
     * @param project_id The project in which the repository is used.
     * @return The repo ID if found, otherwise 0.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public int check_repo(String name, int project_id) throws SQLException, PropertyVetoException {
        fillNameCache();
        
        String key = name.toUpperCase().trim();
        if (nameCache.containsKey(project_id)) {
            Integer cacheId = nameCache.get(project_id).get(key);
            if (cacheId != null) {
                return cacheId;
            }
        }
        
        Integer idRepo = null;
        getCheckRepoStmt();
        
        checkRepoStmt.setString(1, key);
        checkRepoStmt.setInt(2, project_id);
        try (ResultSet rs = checkRepoStmt.executeQuery()) {
            while (rs.next()) {
                idRepo = rs.getInt("id");
            }
        }
        
        insertCache(key, project_id, idRepo);
        if (idRepo == null) {
            return 0;
        }

        return idRepo;
    } 
    
    /**
     * Insert the given repository in the gitlab_repo table. 
     * @param repo_id Repository ID from the repo table
     * @param gitlab_id GitLab internal repository ID
     * @param description GitLab description
     * @param create_date Date when the repository was created in GitLab
     * @param archived Whether the repository is archived (read-only) in GitLab
     * @param has_avatar Whether the repository has an avatar
     * @param star_count Number of stars from developers
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_gitlab_repo(int repo_id, int gitlab_id, String description, Timestamp create_date, boolean archived, boolean has_avatar, int star_count) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertGitLabRepoStmt.getPreparedStatement();
        
        pstmt.setInt(1, repo_id);
        pstmt.setInt(2, gitlab_id);
        if (description != null) {
            pstmt.setString(3, description);
        }
        else {
            pstmt.setNull(3, java.sql.Types.VARCHAR);
        }
        
        pstmt.setTimestamp(4, create_date);
        pstmt.setBoolean(5, archived);
        pstmt.setBoolean(6, has_avatar);
        pstmt.setInt(7, star_count);
        
        insertGitLabRepoStmt.batch();
    }
    
    private void getCheckGitLabRepoStmt() throws SQLException, PropertyVetoException {
        if (checkGitLabRepoStmt == null) {
            Connection con = insertGitLabRepoStmt.getConnection();
            String sql = "SELECT description,create_date,archived,has_avatar,star_count FROM gros.gitlab_repo WHERE repo_id=? AND gitlab_id=?";
            checkGitLabRepoStmt = con.prepareStatement(sql);
        }
    }

    /**
     * Check whether the given repository exists in the gitlab_repo table and
     * has the same properties.
     * @param repo_id Repository ID from the repo table
     * @param gitlab_id GitLab internal repository ID
     * @param description GitLab description
     * @param create_date Date when the repository was created in GitLab
     * @param archived Whether the repository is archived (read-only) in GitLab
     * @param has_avatar Whether the repository has an avatar
     * @param star_count Number of stars from developers
     * @return An indicator of the state of the database regarding the given GitLab
     * repository. This is CheckResult.MISSING if the repository with the provided
     * repo and gitlab identifiers does not exist. This is CheckResult.DIFFERS if
     * there is a row with the provided repo and gitlab identifiers in the database,
     * but it has different values in its fields. This is CheckResult.EXISTS if
     * there is a repository in the database that matches the provided parameters.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public CheckResult check_gitlab_repo(int repo_id, int gitlab_id, String description, Timestamp create_date, boolean archived, boolean has_avatar, int star_count) throws SQLException, PropertyVetoException {
        getCheckGitLabRepoStmt();
        
        checkGitLabRepoStmt.setInt(1, repo_id);
        checkGitLabRepoStmt.setInt(2, gitlab_id);
        CheckResult result;
        try (ResultSet rs = checkGitLabRepoStmt.executeQuery()) {
            result = CheckResult.MISSING;
            while (rs.next()) {
                if ((description == null ? rs.getString("description") == null : description.equals(rs.getString("description"))) &&
                        create_date.equals(rs.getTimestamp("create_date")) &&
                        archived == rs.getBoolean("archived") &&
                        has_avatar == rs.getBoolean("has_avatar") &&
                        star_count == rs.getInt("star_count")) {
                    result = CheckResult.EXISTS;
                }
                else {
                    result = CheckResult.DIFFERS;
                }
            }
        }
        
        return result;
    }
    
    /**
     * Update the given repository in the gitlab_repo table with new values.
     * @param repo_id Repository ID from the repo table
     * @param gitlab_id GitLab internal repository ID
     * @param description GitLab description
     * @param create_date Date when the repository was created in GitLab
     * @param archived Whether the repository is archived (read-only) in GitLab
     * @param has_avatar Whether the repository has an avatar
     * @param star_count Number of stars from developers
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void update_gitlab_repo(int repo_id, int gitlab_id, String description, Timestamp create_date, boolean archived, boolean has_avatar, int star_count) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = updateGitLabRepoStmt.getPreparedStatement();
        if (description != null) {
            pstmt.setString(1, description);
        }
        else {
            pstmt.setNull(1, java.sql.Types.VARCHAR);
        }
        
        pstmt.setTimestamp(2, create_date);
        pstmt.setBoolean(3, archived);
        pstmt.setBoolean(4, has_avatar);
        pstmt.setInt(5, star_count);
        
        pstmt.setInt(6, repo_id);
        pstmt.setInt(7, gitlab_id);

        updateGitLabRepoStmt.batch();
    }
}
    
