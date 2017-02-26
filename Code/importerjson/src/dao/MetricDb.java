/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import java.beans.PropertyVetoException;
import java.io.IOException;
import util.BaseDb;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;

/**
 *
 * @author Enrique and Leon Helwerda
 */
public class MetricDb extends BaseDb implements AutoCloseable {
    PreparedStatement checkMetricStmt = null;
    BatchedStatement insertMetricStmt = null;
    BatchedStatement insertMetricValueStmt = null;
    PreparedStatement checkMetricVersionStmt = null;
    BatchedStatement insertMetricVersionStmt = null;
    BatchedStatement insertMetricTargetStmt = null;
    HashMap<String, Integer> nameCache = null;
    
    public MetricDb() {
        String sql = "insert into gros.metric(name) values (?);";
        insertMetricStmt = new BatchedStatement(sql);
        sql = "insert into gros.metric_value values (?,?,?,?,?,?);";
        insertMetricValueStmt = new BatchedStatement(sql);
        sql = "insert into gros.metric_version values (?,?,?,?,?);";
        insertMetricVersionStmt = new BatchedStatement(sql);
        sql = "insert into gros.metric_target values (?,?,?,?,?,?,?);";
        insertMetricTargetStmt = new BatchedStatement(sql);
    }
    
    public void insert_metric(String name) throws SQLException, IOException, PropertyVetoException{
        PreparedStatement pstmt = insertMetricStmt.getPreparedStatement();
        
        pstmt.setString(1, name);
        
        // Insert immediately because we need to have the row available
        pstmt.execute();
    }
    
    public void insert_metricValue(int id, Integer value, String category, String date, String since_date, int project) throws SQLException, IOException, PropertyVetoException{
        PreparedStatement pstmt = insertMetricValueStmt.getPreparedStatement();
        
        pstmt.setInt(1, id);
        pstmt.setInt(2, value);
        pstmt.setString(3, category);
        pstmt.setTimestamp(4, Timestamp.valueOf(date));
        pstmt.setTimestamp(5, Timestamp.valueOf(since_date));
        pstmt.setInt(6, project);
                    
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
    
    private void getCheckMetricStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkMetricStmt == null) {
            Connection con = insertMetricStmt.getConnection();
            String sql = "SELECT metric_id FROM gros.metric WHERE UPPER(name) = ?";
            checkMetricStmt = con.prepareStatement(sql);
        }
    }
    
    private void fillNameCache() throws SQLException, IOException, PropertyVetoException {
        if (nameCache != null) {
            return;
        }
        nameCache = new HashMap<>();
        
        Connection con = insertMetricStmt.getConnection();
        String sql = "SELECT UPPER(name), metric_id FROM gros.metric";
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
    
    public int check_metric(String name) throws SQLException, IOException, PropertyVetoException {
        fillNameCache();
        
        String key = name.toUpperCase().trim();
        Integer cacheId = nameCache.get(key);
        if (cacheId != null) {
            return cacheId;
        }

        Integer idMetric = null;
        getCheckMetricStmt();
        ResultSet rs = null;
        
        checkMetricStmt.setString(1, key);
        rs = checkMetricStmt.executeQuery();
        
        while (rs.next()) {
            idMetric = rs.getInt("metric_id");
        }
        
        rs.close();
        
        nameCache.put(key, idMetric);
        
        if (idMetric == null) {
            return 0;
        }
        
        return idMetric;
    }

    private void getCheckVersionStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkMetricVersionStmt == null) {
            Connection con = insertMetricStmt.getConnection();
            String sql = "SELECT version_id FROM gros.metric_version WHERE project_id = ? AND version_id = ?";
            checkMetricVersionStmt = con.prepareStatement(sql);
        }
    }
    
    public int check_version(int projectId, int version) throws SQLException, IOException, PropertyVetoException {
        getCheckVersionStmt();
        int idVersion = 0;
        ResultSet rs = null;
        
        checkMetricVersionStmt.setInt(1, projectId);
        checkMetricVersionStmt.setInt(2, version);
        rs = checkMetricVersionStmt.executeQuery();
        
        while (rs.next()) {
            idVersion = rs.getInt("version_id");
        }
        
        rs.close();
        
        return idVersion;
    }

    public void insert_version(int projectId, int version, String developer, String message, String commit_date) throws SQLException, IOException, PropertyVetoException {
        PreparedStatement pstmt = insertMetricVersionStmt.getPreparedStatement();
        
        pstmt.setInt(1, projectId);
        pstmt.setInt(2, version);
        pstmt.setString(3, developer);
        pstmt.setString(4, message);
        pstmt.setTimestamp(5, Timestamp.valueOf(commit_date));
                    
        insertMetricVersionStmt.batch();
    }

    public void insert_target(int projectId, int version, int metricId, String type, int target, int low_target, String comment) throws SQLException, IOException, PropertyVetoException {
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
    
