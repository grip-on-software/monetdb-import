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
        insertStmt = new BatchedStatement("insert into gros.source_environment(project_id,source_type,url,environment,version) values (?,?,?,?,?)");
        updateStmt = new BatchedStatement("update gros.source_environment set source_type = ?, url = ?, version = ? where project_id = ? and environment = ?");
    }

    /**
     * Insert a source environment into the database
     * @param project The identifier of the project for which the environment exists
     * @param type The type of the representative source of the environment
     * @param url The URL of the environment
     * @param environment The environment descriptor, as a serialized string
     * @param version The version of the representative source of the environment
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_source(int project, String type, String url, String environment, String version) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertStmt.getPreparedStatement();

        pstmt.setInt(1, project);
        pstmt.setString(2, type);
        pstmt.setString(3, url);
        pstmt.setString(4, environment);
        pstmt.setString(5, version == null ? "" : version);
        
        insertStmt.batch();
    }
    
    /**
     * Update an existing source environment in the database
     * @param project The identifier of the project for which the environment exists
     * @param type The type of the representative source of the environment
     * @param url The new URL of the environment
     * @param environment The environment descriptor, as a serialized string
     * @param version The version of the representative source of the environment
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void update_source(int project, String type, String url, String environment, String version) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = updateStmt.getPreparedStatement();

        pstmt.setString(1, type);
        pstmt.setString(2, url);
        pstmt.setString(3, version == null ? "" : version);

        pstmt.setInt(4, project);
        pstmt.setString(5, environment);
        
        insertStmt.batch();
    }
    
    private void getCheckStmt() throws SQLException, PropertyVetoException {
        if (checkStmt == null) {
            Connection con = insertStmt.getConnection();
            checkStmt = con.prepareStatement("select source_type, url, version from gros.source_environment where project_id = ? and environment = ?");
        }
    }

    /**
     * Check if a source environment exists in the database
     * @param project The identifier of the project for which the environment exists
     * @param type The type of the representative source of the environment
     * @param url The URL of the environment
     * @param environment The environment descriptor, as a serialized string
     * @param version The version of the representative source of the environment
     * @return Whether the environment exists with the same URL
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public CheckResult check_source(int project, String type, String url, String environment, String version) throws SQLException, PropertyVetoException {
        getCheckStmt();
        
        checkStmt.setInt(1, project);
        checkStmt.setString(2, environment);
        
        try (ResultSet rs = checkStmt.executeQuery()) {
            if (rs.next()) {
                if (type.equals(rs.getString("source_type")) &&
                        url.equals(rs.getString("url")) &&
                        (version == null || version.isEmpty() || version.equals(rs.getString("version")))) {
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
