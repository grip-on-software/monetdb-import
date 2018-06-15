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
 * Database access management for the source environments.
 * @author Leon Helwerda
 */
public class EnvironmentDb extends BaseDb implements AutoCloseable {
    private final BatchedStatement insertStmt;
    private final BatchedStatement updateStmt;
    private PreparedStatement checkStmt = null;
    
    public enum CheckResult {
        MISSING, DIFFERS, EXISTS
    }

    public EnvironmentDb() {
        insertStmt = new BatchedStatement("insert into gros.source_environment(project_id,source_type,url,environment) values (?,?,?,?)");
        updateStmt = new BatchedStatement("update gros.source_environment set source_type = ?, url = ? where project_id = ? and environment = ?");
    }

    /**
     * Insert a source environment into the database
     * @param project The identifier of the project for which the environment exists
     * @param type The type of the representative source of the environment
     * @param url The URL of the environment
     * @param environment The environment descriptor, as a serialized string
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_source(int project, String type, String url, String environment) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertStmt.getPreparedStatement();

        pstmt.setInt(1, project);
        pstmt.setString(2, type);
        pstmt.setString(3, url);
        pstmt.setString(4, environment);
        
        insertStmt.batch();
    }
    
    /**
     * Update an existing source environment in the database
     * @param project The identifier of the project for which the environment exists
     * @param type The type of the representative source of the environment
     * @param url The new URL of the environment
     * @param environment The environment descriptor, as a serialized string
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void update_source(int project, String type, String url, String environment) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = updateStmt.getPreparedStatement();

        pstmt.setString(1, url);

        pstmt.setInt(2, project);
        pstmt.setString(3, type);
        pstmt.setString(4, environment);
        
        insertStmt.batch();
    }
    
    private void getCheckStmt() throws SQLException, PropertyVetoException {
        if (checkStmt == null) {
            Connection con = insertStmt.getConnection();
            checkStmt = con.prepareStatement("select source_type, url from gros.source_environment where project_id = ? and environment = ?");
        }
    }

    /**
     * Check if a source environment exists in the database
     * @param project The identifier of the project for which the environment exists
     * @param type The type of the representative source of the environment
     * @param url The URL of the environment
     * @param environment The environment descriptor, as a serialized string
     * @return Whether the environment exists with the same URL
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public CheckResult check_source(int project, String type, String url, String environment) throws SQLException, PropertyVetoException {
        getCheckStmt();
        
        checkStmt.setInt(1, project);
        checkStmt.setString(2, environment);
        
        try (ResultSet rs = checkStmt.executeQuery()) {
            if (rs.next()) {
                if (type.equals(rs.getString("source_type")) && url.equals("url")) {
                    return CheckResult.EXISTS;
                }
                return CheckResult.DIFFERS;
            }
        }
        
        return CheckResult.MISSING;
    }

    @Override
    public void close() throws Exception {
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
