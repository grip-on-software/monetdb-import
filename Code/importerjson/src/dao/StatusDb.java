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
import util.BaseDb;

/**
 * Database access management for JIRA statuses.
 * @author Leon Helwerda
 */
public class StatusDb extends BaseDb implements AutoCloseable {
    private BatchedStatement insertStatusStmt = null;
    private BatchedStatement updateStatusStmt = null;
    private PreparedStatement checkStatusStmt = null;
    private BatchedStatement insertCategoryStmt = null;
    private BatchedStatement updateCategoryStmt = null;
    private PreparedStatement checkCategoryStmt = null;

    public enum CheckResult {
        MISSING, DIFFERS, EXISTS
    }
    
    public StatusDb() {
        insertStatusStmt = new BatchedStatement("insert into gros.status(id,name,description,category_id) values (?,?,?,?)");
        updateStatusStmt = new BatchedStatement("update gros.status set name=?, description=?, category_id=? where id=?");
        
        insertCategoryStmt = new BatchedStatement("insert into gros.status_category(category_id,key,name,color) values (?,?,?,?)");
        updateCategoryStmt = new BatchedStatement("update gros.status_category set key=?, name=?, color=? where category_id=?");
    }

    @Override
    public void close() throws SQLException {
        insertStatusStmt.execute();
        insertStatusStmt.close();
        
        updateStatusStmt.execute();
        updateStatusStmt.close();
        
        if (checkStatusStmt != null) {
            checkStatusStmt.close();
            checkStatusStmt = null;
        }
        
        insertCategoryStmt.execute();
        insertCategoryStmt.close();
        
        updateCategoryStmt.execute();
        updateCategoryStmt.close();
        
        if (checkCategoryStmt != null) {
            checkCategoryStmt.close();
            checkCategoryStmt = null;
        }
    }
    
    private void getCheckStatusStmt() throws SQLException, PropertyVetoException {
        if (checkStatusStmt == null) {
            Connection con = insertStatusStmt.getConnection();
            checkStatusStmt = con.prepareStatement("select name, description, category_id from gros.status where id=?");
        }
    }

    /**
     * Check whether a status exists in the database.
     * @param id Identifier of the status
     * @param name Name of the status
     * @param description Description of the status
     * @param category_id Category identifier of the status or NULL if not known
     * @return An indicator of the state of the database regarding the given status.
     * This is CheckResult.MISSING if the status with the provided identifier
     * does not exist. This is CheckResult.DIFFERS if there is a row with the
     * provided identifier in the database, but it has different values
     * in its fields. This is CheckResult.EXISTS if there is a status in the database
     * that matches all the provided parameters.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public CheckResult check_status(int id, String name, String description, Integer category_id) throws SQLException, PropertyVetoException {
        getCheckStatusStmt();
        
        checkStatusStmt.setInt(1, id);
        
        try (ResultSet rs = checkStatusStmt.executeQuery()) {
            if (rs.next()) {
                if (name.equals(rs.getString("name")) &&
                        description.equals(rs.getString("description")) &&
                        category_id == null ? rs.getObject("category_id") == null : category_id == rs.getInt("category_id")) {
                    return CheckResult.EXISTS;
                }
                else {
                    return CheckResult.DIFFERS;
                }
            }
        }
        
        return CheckResult.MISSING;
    }

    /**
     * Insert a new status in the database.
     * @param id Identifier of the status
     * @param name Name of the status
     * @param description Description of the status
     * @param category_id Category identifier of the status or NULL if not known
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_status(int id, String name, String description, Integer category_id) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertStatusStmt.getPreparedStatement();
        
        pstmt.setInt(1, id);
        pstmt.setString(2, name);
        pstmt.setString(3, description);
        setInteger(pstmt, 4, id);
        
        insertStatusStmt.batch();
    }

    /**
     * Update an existing status in the database.
     * @param id Identifier of the status
     * @param name Name of the status
     * @param description Description of the status
     * @param category_id Category identifier of the status or NULL if not known
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void update_status(int id, String name, String description, Integer category_id) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = updateStatusStmt.getPreparedStatement();
        
        pstmt.setString(1, name);
        pstmt.setString(2, description);
        setInteger(pstmt, 3, category_id);
        
        pstmt.setInt(4, id);
        
        updateStatusStmt.batch();
    }
    
    private void getCheckCategoryStmt() throws SQLException, PropertyVetoException {
        if (checkCategoryStmt == null) {
            Connection con = insertCategoryStmt.getConnection();
            checkCategoryStmt = con.prepareStatement("select key, name, color from gros.status_category where category_id=?");
        }
    }
    
    /**
     * Check whether a status category exists in the database.
     * @param category_id Identifier of the status category
     * @param key Lowercase unique string describing the status category
     * @param name Human-readable name describing the status category
     * @param color Color name of the status category
     * @return An indicator of the state of the database regarding the given category.
     * This is CheckResult.MISSING if the status with the provided identifier
     * does not exist. This is CheckResult.DIFFERS if there is a row with the
     * provided identifier in the database, but it has different values
     * in its fields. This is CheckResult.EXISTS if there is a status category
     * in the database that matches all the provided parameters.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public CheckResult check_category(int category_id, String key, String name, String color) throws SQLException, PropertyVetoException {
        getCheckCategoryStmt();
        
        checkCategoryStmt.setInt(1, category_id);
        
        try (ResultSet rs = checkCategoryStmt.executeQuery()) {
            if (rs.next()) {
                if (key.equals(rs.getString("key")) &&
                        name.equals(rs.getString("name")) &&
                        color.equals(rs.getString("color"))) {
                    return CheckResult.EXISTS;
                }
                else {
                    return CheckResult.DIFFERS;
                }
            }
        }
        
        return CheckResult.MISSING;        
    }
    
    /**
     * Insert a new status category in the database.
     * @param category_id Identifier of the status category
     * @param key Lowercase unique string describing the status category
     * @param name Human-readable name describing the status category
     * @param color Color name of the status category
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_category(int category_id, String key, String name, String color) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertCategoryStmt.getPreparedStatement();
        
        pstmt.setInt(1, category_id);
        pstmt.setString(2, key);
        pstmt.setString(3, name);
        pstmt.setString(4, color);
        
        insertCategoryStmt.batch();
    }
    
    /**
     * Update an existing status category in the database.
     * @param category_id Identifier of the status category
     * @param key Lowercase unique string describing the status category
     * @param name Human-readable name describing the status category
     * @param color Color name of the status category
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void update_category(int category_id, String key, String name, String color) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = updateCategoryStmt.getPreparedStatement();
        
        pstmt.setString(1, key);
        pstmt.setString(2, name);
        pstmt.setString(3, color);

        pstmt.setInt(4, category_id);
        
        updateCategoryStmt.batch();
    }
    
}
