/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import java.beans.PropertyVetoException;
import util.BaseDb;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;

/**
 * Database access management for the metrics tables.
 * @author Enrique, Leon Helwerda
 */
public class MetricDb extends BaseDb implements AutoCloseable {
    private PreparedStatement checkMetricStmt = null;
    private BatchedStatement insertMetricStmt = null;
    private BatchedStatement insertMetricValueStmt = null;
    private PreparedStatement checkMetricVersionStmt = null;
    private BatchedStatement insertMetricVersionStmt = null;
    private BatchedStatement insertMetricTargetStmt = null;
    private HashMap<String, Integer> nameCache = null;
    
    public MetricDb() {
        String sql = "insert into gros.metric(name) values (?);";
        insertMetricStmt = new BatchedStatement(sql);
        sql = "insert into gros.metric_value(metric_id,value,category,date,sprint_id,since_date,project_id) values (?,?,?,?,?,?,?);";
        insertMetricValueStmt = new BatchedStatement(sql);
        sql = "insert into gros.metric_version(project_id,version_id,developer,message,commit_date) values (?,?,?,?,?);";
        insertMetricVersionStmt = new BatchedStatement(sql);
        sql = "insert into gros.metric_target(project_id,version_id,metric_id,type,target,low_target,comment) values (?,?,?,?,?,?,?);";
        insertMetricTargetStmt = new BatchedStatement(sql);
    }
    
    /**
     * Insert a metric name into the metrics table.
     * @param name The name of the metric, possibly including project-specific 
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_metric(String name) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertMetricStmt.getPreparedStatement();
        
        pstmt.setString(1, name);
        
        // Insert immediately because we need to have the row available
        pstmt.execute();
    }
    
    /**
     * Insert a metric measurement into the metric values table.
     * @param metric_id Identifier of the metric name
     * @param value The value of the metric at time of measurement
     * @param category The category related to the metric's value ('red', 'green', 'yellow')
     * @param date Timestamp at which the measurement took place
     * @param sprint_id Identifier of the sprint in which the measurement took place
     * @param since_date Timestamp since which the metric has the same value
     * @param project Identifier of the project in which the measurement was made
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_metricValue(int metric_id, int value, String category, Timestamp date, int sprint_id, Timestamp since_date, int project) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertMetricValueStmt.getPreparedStatement();
        
        pstmt.setInt(1, metric_id);
        pstmt.setInt(2, value);
        pstmt.setString(3, category);
        pstmt.setTimestamp(4, date);
        pstmt.setInt(5, sprint_id);
        setTimestamp(pstmt, 6, since_date);
        pstmt.setInt(7, project);
                    
        insertMetricValueStmt.batch();
    }
    
    @Override
    public void close() throws SQLException {
        // All metric names are already commited
        insertMetricStmt.close();
        
        insertMetricValueStmt.execute();
        insertMetricValueStmt.close();
        
        if (checkMetricStmt != null) {
            checkMetricStmt.close();
            checkMetricStmt = null;
        }
        
        if (checkMetricVersionStmt != null) {
            checkMetricVersionStmt.close();
            checkMetricVersionStmt = null;
        }
        insertMetricVersionStmt.execute();
        insertMetricVersionStmt.close();
        
        insertMetricTargetStmt.execute();
        insertMetricTargetStmt.close();
        
        if (nameCache != null) {
            nameCache.clear();
            nameCache = null;
        }
    }
    
    private void getCheckMetricStmt() throws SQLException, PropertyVetoException {
        if (checkMetricStmt == null) {
            Connection con = insertMetricStmt.getConnection();
            String sql = "SELECT metric_id FROM gros.metric WHERE UPPER(name) = ?";
            checkMetricStmt = con.prepareStatement(sql);
        }
    }
    
    private void fillNameCache() throws SQLException, PropertyVetoException {
        if (nameCache != null) {
            return;
        }
        nameCache = new HashMap<>();
        
        Connection con = insertMetricStmt.getConnection();
        String sql = "SELECT UPPER(name), metric_id FROM gros.metric";
        
        try (
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql)
        ) {
            while(rs.next()) {
                String key = rs.getString(1);
                Integer id = Integer.parseInt(rs.getString(2));
                nameCache.put(key, id);
            }
        }
    }
    
    /**
     * Check whether a metric name exists in the database and return its identifier.
     * @param name Name of the metric
     * @return Identifier of the metric, or 0 if it is not found
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public int check_metric(String name) throws SQLException, PropertyVetoException {
        fillNameCache();
        
        String key = name.toUpperCase().trim();
        Integer cacheId = nameCache.get(key);
        if (cacheId != null) {
            return cacheId;
        }

        Integer idMetric = null;
        getCheckMetricStmt();
        
        checkMetricStmt.setString(1, key);
        try (ResultSet rs = checkMetricStmt.executeQuery()) {
            while (rs.next()) {
                idMetric = rs.getInt("metric_id");
            }
        }
        
        nameCache.put(key, idMetric);
        
        if (idMetric == null) {
            return 0;
        }
        
        return idMetric;
    }

    private void getCheckVersionStmt() throws SQLException, PropertyVetoException {
        if (checkMetricVersionStmt == null) {
            Connection con = insertMetricStmt.getConnection();
            String sql = "SELECT version_id FROM gros.metric_version WHERE project_id = ? AND version_id = ?";
            checkMetricVersionStmt = con.prepareStatement(sql);
        }
    }
    
    /**
     * Check whether a metric version exists in the database.
     * @param projectId Identifier of the project to which the metric version change applies
     * @param version The revision number of the change
     * @return The revision number if the version exists, or 0 if it is not found
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public int check_version(int projectId, int version) throws SQLException, PropertyVetoException {
        getCheckVersionStmt();
        int idVersion = 0;
        
        checkMetricVersionStmt.setInt(1, projectId);
        checkMetricVersionStmt.setInt(2, version);
        try (ResultSet rs = checkMetricVersionStmt.executeQuery()) {
            while (rs.next()) {
                idVersion = rs.getInt("version_id");
            }
        }
        
        return idVersion;
    }

    /**
     * Insert a new metric version in the database
     * @param projectId Identifier of the project to which the metric version change applies
     * @param version The revision number of the change
     * @param developer Shorthand name of the developer that made the change
     * @param message Commit message describing the change 
     * @param commit_date Timestamp at which the target change took place
     * @param sprint_id Identifier of the sprint in which the target norms were changed
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_version(int projectId, int version, String developer, String message, Timestamp commit_date, int sprint_id) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertMetricVersionStmt.getPreparedStatement();
        
        pstmt.setInt(1, projectId);
        pstmt.setInt(2, version);
        pstmt.setString(3, developer);
        pstmt.setString(4, message);
        pstmt.setTimestamp(5, commit_date);
        pstmt.setInt(6, sprint_id);
                    
        insertMetricVersionStmt.batch();
    }

    /**
     * Insert a new project-specific target norm change in the metric targets table.
     * @param projectId Identifier of the project to which the metric norm change applies
     * @param version The revision number of the change
     * @param metricId Identifier of the metric name
     * @param type The type of norm change: 'options', 'old_options', 'TechnicalDebtTarget' or 'DynamicTechnicalDebtTarget'
     * @param target The norm value at which the category changes from green to yellow
     * @param low_target The norm value at which the category changes from yellow to red
     * @param comment Comment for technical debt targets describing the reason of the norm change
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_target(int projectId, int version, int metricId, String type, int target, int low_target, String comment) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertMetricTargetStmt.getPreparedStatement();
        
        pstmt.setInt(1, projectId);
        pstmt.setInt(2, version);
        pstmt.setInt(3, metricId);
        pstmt.setString(4, type);
        pstmt.setInt(5, target);
        pstmt.setInt(6, low_target);
        pstmt.setString(7, comment);
        
        insertMetricTargetStmt.batch();
    }
        

}
    
