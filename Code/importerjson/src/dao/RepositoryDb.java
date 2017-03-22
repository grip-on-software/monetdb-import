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
import java.sql.Timestamp;
import java.util.HashMap;
import util.BaseDb;

/**
 * Class is created to manage the repository table of the database.
 * @author Enrique & Thomas
 */
public class RepositoryDb extends BaseDb implements AutoCloseable {
    
    BatchedStatement insertRepoStmt = null;
    PreparedStatement checkRepoStmt = null;
    HashMap<String, Integer> nameCache = null;
    BatchedStatement insertGitLabRepoStmt = null;
    PreparedStatement checkGitLabRepoStmt = null;
    BatchedStatement updateGitLabRepoStmt = null;
    
    public enum CheckResult {
        MISSING, DIFFERS, EXISTS
    };
    
    public RepositoryDb() {
        String sql = "insert into gros.repo (repo_name) values (?);";
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
        
        if (checkGitLabRepoStmt != null) {
            checkGitLabRepoStmt.close();
            checkGitLabRepoStmt = null;
        }
    }
    
    /**
     * Inserts repository in the repo table. 
     * @param name The complete name of the repository.
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.beans.PropertyVetoException
     */
    public void insert_repo(String name) throws SQLException, IOException, PropertyVetoException{
        PreparedStatement pstmt = insertRepoStmt.getPreparedStatement();
        
        pstmt.setString(1, name);
        
        // Insert immediately because we need to have the row available
        pstmt.execute();
    }
    
    private void getCheckRepoStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkRepoStmt == null) {
            Connection con = insertRepoStmt.getConnection();
            String sql = "SELECT id FROM gros.repo WHERE UPPER(repo_name) = ?";
            checkRepoStmt = con.prepareStatement(sql);
        }
    }
    
    private void fillNameCache() throws SQLException, IOException, PropertyVetoException {
        if (nameCache != null) {
            return;
        }
        nameCache = new HashMap<>();
        
        Connection con = insertRepoStmt.getConnection();
        String sql = "SELECT UPPER(repo_name), id FROM gros.repo";
        try (Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {
            while(rs.next()) {
                String key = rs.getString(1);
                Integer id = Integer.parseInt(rs.getString(2));
                nameCache.put(key, id);
            }
        }
    }
    
    /**
     * Returns the repository ID if the repository already exists in the repo 
     * table of the database. Otherwise, returns 0.
     * @param name the complete name of the repository.
     * @return the repo ID if found, otherwise 0.
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.beans.PropertyVetoException
     */
    public int check_repo(String name) throws SQLException, IOException, PropertyVetoException {
        fillNameCache();
        
        String key = name.toUpperCase().trim();
        Integer cacheId = nameCache.get(key);
        if (cacheId != null) {
            return cacheId;
        }
        
        Integer idRepo = null;
        getCheckRepoStmt();
        
        checkRepoStmt.setString(1, key);
        try (ResultSet rs = checkRepoStmt.executeQuery()) {
            while (rs.next()) {
                idRepo = rs.getInt("id");
            }
        }
        
        nameCache.put(key, idRepo);
        if (idRepo == null) {
            return 0;
        }

        return idRepo;
    } 
    
    /**
     * Inserts repository in the gitlab_repo table. 
     * @param repo_id Repository ID from the repo table
     * @param gitlab_id GitLab internal repository ID
     * @param description GitLab description
     * @param create_date Date when the repository was created in GitLab
     * @param archived Whether the repository is archived (read-only) in GitLab
     * @param has_avatar Whether the repository has an avatar
     * @param star_count Number of stars from developers
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.beans.PropertyVetoException
     */
    public void insert_gitlab_repo(int repo_id, int gitlab_id, String description, Timestamp create_date, boolean archived, boolean has_avatar, int star_count) throws SQLException, IOException, PropertyVetoException{
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
    
    private void getCheckGitLabRepoStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkGitLabRepoStmt == null) {
            Connection con = insertGitLabRepoStmt.getConnection();
            String sql = "SELECT description,create_date,archived,has_avatar,star_count FROM gros.gitlab_repo WHERE repo_id=? AND gitlab_id=?";
            checkGitLabRepoStmt = con.prepareStatement(sql);
        }
    }

    public CheckResult check_gitlab_repo(int repo_id, int gitlab_id, String description, Timestamp create_date, boolean archived, boolean has_avatar, int star_count) throws SQLException, IOException, PropertyVetoException{
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
    
    public void update_gitlab_repo(int repo_id, int gitlab_id, String description, Timestamp create_date, boolean archived, boolean has_avatar, int star_count) throws SQLException, IOException, PropertyVetoException{
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
    
