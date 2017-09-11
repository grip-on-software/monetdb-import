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
import util.BaseDb;

/**
 * Database access management for the source environments.
 * @author Leon Helwerda
 */
public class EnvironmentDb extends BaseDb implements AutoCloseable {
    private BatchedStatement insertStmt = null;
    private PreparedStatement checkStmt = null;
    
    public EnvironmentDb() {
        insertStmt = new BatchedStatement("insert into gros.source_environment(project_id,source_type,url,environment) values (?,?,?,?)");
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
    
    private void getCheckStmt() throws SQLException, PropertyVetoException {
        if (checkStmt == null) {
            Connection con = insertStmt.getConnection();
            checkStmt = con.prepareStatement("select source_type from gros.source_environment where project_id = ? and environment = ?");
        }
    }

    /**
     * Check if a source environment exists in the database
     * @param project The identifier of the project for which the environment exists
     * @param environment The environment descriptor, as a serialized string
     * @return Whether the environment exists
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public boolean check_source(int project, String environment) throws SQLException, PropertyVetoException {
        getCheckStmt();
        
        checkStmt.setInt(1, project);
        checkStmt.setString(2, environment);
        
        try (ResultSet rs = checkStmt.executeQuery()) {
            if (rs.next()) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public void close() throws Exception {
        insertStmt.execute();
        insertStmt.close();
        
        if (checkStmt != null) {
            checkStmt.close();
            checkStmt = null;
        }
    }
    
}
