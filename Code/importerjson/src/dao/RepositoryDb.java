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
 * Class is created to manage the repository table of the database.
 * @author Enrique & Thomas
 */
public class RepositoryDb extends BaseDb {
    
    BatchedStatement bstmt = null;
    PreparedStatement checkRepoStmt = null;
    HashMap<String, Integer> nameCache = null;
    
    public RepositoryDb() {
        String sql = "insert into gros.repo (repo_name) values (?);";
        bstmt = new BatchedStatement(sql);
    }
    
    public void close() throws SQLException {
        // All statements are already executed
        bstmt.close();
        
        if (checkRepoStmt != null) {
            checkRepoStmt.close();
            checkRepoStmt = null;
        }
        
        if (nameCache != null) {
            nameCache.clear();
            nameCache = null;
        }
    }
    
    /**
     * Inserts repository in the developer table. 
     * @param name The complete name of the repository.
     */
    public void insert_repo(String name) throws SQLException, IOException, PropertyVetoException{
        PreparedStatement pstmt = bstmt.getPreparedStatement();
        
        pstmt.setString(1, name);
        
        // Insert immediately because we need to have the row available
        pstmt.execute();
    }
    
    private void getCheckRepoStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkRepoStmt == null) {
            Connection con = bstmt.getConnection();
            String sql = "SELECT id FROM gros.repo WHERE UPPER(repo_name) = ?";
            checkRepoStmt = con.prepareStatement(sql);
        }
    }
    
    private void fillNameCache() throws SQLException, IOException, PropertyVetoException {
        if (nameCache != null) {
            return;
        }
        nameCache = new HashMap<>();
        
        Connection con = bstmt.getConnection();
        String sql = "SELECT UPPER(repo_name), id FROM gros.repo";
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        while(rs.next()) {
            String key = rs.getString(1);
            Integer id = Integer.parseInt(rs.getString(2));
            nameCache.put(key, id);
        }

        stmt.close();
        rs.close();
    }
    
    /**
     * Returns the developer ID if the developer already exists in the developer 
     * table of the database. Else returns 0.
     * @param name the complete name of the repository.
     * @return the Developer ID if found, otherwise 0.
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
        ResultSet rs = null;
        
        checkRepoStmt.setString(1, key);
        rs = checkRepoStmt.executeQuery();
 
        while (rs.next()) {
            idRepo = rs.getInt("id");
        }
        
        rs.close();
        
        nameCache.put(key, idRepo);
        if (idRepo == null) {
            return 0;
        }

        return idRepo;
    } 
    
}
    
