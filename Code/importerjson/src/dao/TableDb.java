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

/**
 * Database access object for metadata tables that only have an id--name relation,
 * plus optionally some additional metadata field.
 * @author Leon Helwerda
 */
public class TableDb implements AutoCloseable {
    String tableName;
    String fieldName;
    int numberOfFields = 2;
    BatchedStatement insertStmt = null;
    PreparedStatement checkIdStmt = null;
    PreparedStatement checkValueStmt = null;
    
    /**
     * Create a database access object for metadata tables
     * @param tableName Table name in the database
     * @param fieldName Field name of the name field in the database
     * @param metadataFieldName Field name of the metadata field in the database
     */
    public TableDb(String tableName, String fieldName, String metadataFieldName) {
        this.tableName = tableName;
        this.fieldName = fieldName;
        
        String fieldNames = "id," + fieldName;
        String fields = "?,?";
        if (metadataFieldName != null) {
            fieldNames += "," + metadataFieldName;
            fields += ",?";
            numberOfFields++;
        }
        String sql = "insert into gros." + tableName + "(" + fieldNames + ") values (" + fields + ")";
        insertStmt = new BatchedStatement(sql);
    }
    
    @Override
    public void close() throws Exception {
        insertStmt.execute();
        insertStmt.close();
        
        if (checkIdStmt != null) {
            checkIdStmt.close();
            checkIdStmt = null;
        }
        if (checkValueStmt != null) {
            checkValueStmt.close();
            checkValueStmt = null;
        }
    }
    
    private void getCheckIdStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkIdStmt == null) {
            Connection con = insertStmt.getConnection();
            String sql = "SELECT id FROM gros." + this.tableName + " WHERE id = ?";
            checkIdStmt = con.prepareStatement(sql);
        }
    }

    private void getCheckValueStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkValueStmt == null) {
            Connection con = insertStmt.getConnection();
            String sql = "SELECT id FROM gros." + this.tableName + " WHERE UPPER(" + this.fieldName + ") = ?";
            checkValueStmt = con.prepareStatement(sql);
        }
    }
    
    /**
     * Insert an id--name relation into the table
     * @param identifier Identifier
     * @param value Name value
     * @throws SQLException
     * @throws IOException
     * @throws PropertyVetoException 
     */
    public void insert(int identifier, String value) throws SQLException, IOException, PropertyVetoException {
        if (numberOfFields != 2) {
            throw new IllegalArgumentException("must provide " + String.valueOf(numberOfFields) + " arguments, not 2");
        }
        PreparedStatement pstmt = insertStmt.getPreparedStatement();
        
        pstmt.setInt(0, identifier);
        pstmt.setString(1, value);
        
        insertStmt.batch();
    }

    /**
     * Insert an id--name plus additional metadata relation into the table.
     * @param identifier Identifier
     * @param value Name value
     * @param metadata Metdata value
     * @throws SQLException
     * @throws IOException
     * @throws PropertyVetoException 
     */
    public void insert(int identifier, String value, String metadata) throws SQLException, IOException, PropertyVetoException {
        if (numberOfFields != 3) {
            throw new IllegalArgumentException("must provide " + String.valueOf(numberOfFields) + " arguments, not 3");
        }
        PreparedStatement pstmt = insertStmt.getPreparedStatement();
        
        pstmt.setInt(0, identifier);
        pstmt.setString(1, value);
        pstmt.setString(2, metadata);
        
        insertStmt.batch();
    }
    
    /**
     * Check whether a certain identifier has an id--name relation in the table
     * @param identifier The identifier to check for
     * @return The identifier if it exists in the table, otherwise null
     * @throws SQLException
     * @throws IOException
     * @throws PropertyVetoException 
     */
    public Integer check(int identifier) throws SQLException, IOException, PropertyVetoException {
        getCheckIdStmt();
        
        Integer id = null;
        
        checkIdStmt.setInt(0, identifier);
        try (ResultSet rs = checkIdStmt.executeQuery()) {
            while (rs.next()) {
                id = rs.getInt("id");
            }
        }
        
        return id;
    }
    
    /**
     * Check whether a certain name value has an id--name relation in the table
     * @param value The name value to check for
     * @return The identifier if it exists in the table, otherwise null
     * @throws SQLException
     * @throws IOException
     * @throws PropertyVetoException 
     */
    public Integer check(String value) throws SQLException, IOException, PropertyVetoException {
        getCheckValueStmt();

        Integer id = null;
        
        checkValueStmt.setString(0, value);
        try (ResultSet rs = checkValueStmt.executeQuery()) {
            while (rs.next()) {
                id = rs.getInt("id");
            }
        }
        
        return id;
    }
}
