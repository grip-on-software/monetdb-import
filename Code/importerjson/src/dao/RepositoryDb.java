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
import util.BaseImport;

/**
 * Class is created to manage the developer table of the database.
 * @author Enrique & Thomas
 */
public class RepositoryDb extends BaseImport{
    
    BatchedStatement bstmt = null;
    PreparedStatement checkRepoStmt = null;
    
    public RepositoryDb() {
        String sql = "insert into gros.git_repo (git_name) values (?);";
        bstmt = new BatchedStatement(sql);
    }
    
    public void close() throws SQLException {
        // All statements are already executed
        bstmt.close();
        
        if (checkRepoStmt != null) {
            checkRepoStmt.close();
            checkRepoStmt = null;
        }
    }
    
    /**
     * Inserts repository in the developer table. 
     * @param name The complete name of the repository.
     */
    public void insert_repo(String name) throws SQLException, IOException, PropertyVetoException{
        PreparedStatement pstmt = bstmt.getPreparedStatement();
        
        pstmt.setString(1, name);
        pstmt.addBatch();
        
        // Insert immediately because we need to have the row available
        bstmt.execute();
    }
    
    private void getCheckRepoStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkRepoStmt == null) {
            Connection con = bstmt.getConnection();
            String sql = "SELECT id FROM gros.git_repo WHERE UPPER(git_name) = ?";
            checkRepoStmt = con.prepareStatement(sql);
        }
    
    }
    
    /**
     * Returns the developer ID if the developer already exists in the developer 
     * table of the database. Else returns 0.
     * @param name the complete name of the repository.
     * @return the Developer ID if found, otherwise 0.
     */
    public int check_repo(String name) throws SQLException, IOException, PropertyVetoException{
        int idRepo = 0;
        getCheckRepoStmt();
        ResultSet rs = null;
        
        checkRepoStmt.setString(1, name.toUpperCase().trim());
        rs = checkRepoStmt.executeQuery();
 
        while (rs.next()) {
            idRepo = rs.getInt("id");
        }

        return idRepo;
    } 
    
}
    
