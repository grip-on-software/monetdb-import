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
import java.sql.Timestamp;
import util.BaseDb;

/**
 * Database access management for the update trackers table.
 * @author Leon Helwerda
 */
public class UpdateDb extends BaseDb implements AutoCloseable {
    private BatchedStatement insertStmt = null;
    private PreparedStatement checkStmt = null;
    private BatchedStatement updateStmt = null;
    
    public UpdateDb() {
        insertStmt = new BatchedStatement("insert into gros.update_tracker(project_id,filename,contents,update_date) values (?,?,?,?);");
        updateStmt = new BatchedStatement("update gros.update_tracker set contents=?, update_date=? where project_id=? and filename=?");
    }
    
    private void getCheckStmt() throws SQLException, PropertyVetoException {
        if (checkStmt == null) {
            Connection con = insertStmt.getConnection();
            checkStmt = con.prepareStatement("select project_id, filename from gros.update_tracker where project_id=? and filename=?");
        }
    }
    
    /**
     * Check whether an update tracker for a certain project exists in the database.
     * @param project_id Identifier of the project that the file tracks
     * @param filename The name of the file (without path) that keeps track of the update state
     * @return Whether the update tracker file is stored in the database for the given project
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public boolean check_file(int project_id, String filename) throws SQLException, PropertyVetoException {
        getCheckStmt();
        
        checkStmt.setInt(1, project_id);
        checkStmt.setString(2, filename);
        try (ResultSet rs = checkStmt.executeQuery()) {
            if (rs.next()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Insert a new update tracker file in the database.
     * @param project_id Identifier of the project that the file tracks
     * @param filename The name of the file (without path) that keeps track of the update state
     * @param contents Textual contents of the update tracker
     * @param update_date The latest modification date of the file
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_file(int project_id, String filename, String contents, Timestamp update_date) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertStmt.getPreparedStatement();
        
        pstmt.setInt(1, project_id);
        pstmt.setString(2, filename);
        pstmt.setString(3, contents);
        pstmt.setTimestamp(4, update_date);
        
        insertStmt.batch();
    }
    
    /**
     * Update an existing update tracker file in the database with new contents.
     * @param project_id Identifier of the project that the file tracks
     * @param filename The name of the file (without path) that keeps track of the update state
     * @param contents Textual contents of the update tracker
     * @param update_date The latest modification date of the file
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void update_file(int project_id, String filename, String contents, Timestamp update_date) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = updateStmt.getPreparedStatement();
        
        pstmt.setString(1, contents);
        pstmt.setTimestamp(2, update_date);

        pstmt.setInt(3, project_id);
        pstmt.setString(4, filename);
        
        updateStmt.batch();
    }

    @Override
    public void close() throws SQLException {
        insertStmt.execute();
        insertStmt.close();
        
        updateStmt.execute();
        updateStmt.close();
        
        if (checkStmt != null) {
            checkStmt.close();
            checkStmt = null;
        }
    }
    
}
