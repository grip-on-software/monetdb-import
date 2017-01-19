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
 * @author Enrique
 */
public class MetricDb extends BaseDb {
    PreparedStatement checkMetricStmt = null;
    BatchedStatement insertMetricStmt = null;
    BatchedStatement insertMetricValueStmt = null;
    HashMap<String, Integer> nameMap = null;
    
    public MetricDb() {
        String sql = "insert into gros.metric(name) values (?);";
        insertMetricStmt = new BatchedStatement(sql);
        sql = "insert into gros.metric_value values (?,?,?,?,?,?);";
        insertMetricValueStmt = new BatchedStatement(sql);
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
    
    public void close() throws SQLException {
        // All metric names are already commited
        insertMetricStmt.close();
        
        insertMetricValueStmt.execute();
        insertMetricValueStmt.close();
        
        if (checkMetricStmt != null) {
            checkMetricStmt.close();
            checkMetricStmt = null;
        }
        
        if (nameMap != null) {
            nameMap.clear();
            nameMap = null;
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
        if (nameMap != null) {
            return;
        }
        nameMap = new HashMap<>();
        
        Connection con = insertMetricStmt.getConnection();
        String sql = "SELECT UPPER(name), metric_id FROM gros.metric";
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        while(rs.next()) {
            String key = rs.getString(1);
            Integer id = Integer.parseInt(rs.getString(2));
            nameMap.put(key, id);
        }

        stmt.close();
        rs.close();
    }
    
    public int check_metric(String name) throws SQLException, IOException, PropertyVetoException {
        fillNameCache();
        
        String key = name.toUpperCase().trim();
        Integer mapId = nameMap.get(key);
        if (mapId != null) {
            return mapId;
        }

        int idMetric = 0;
        getCheckMetricStmt();
        ResultSet rs = null;
        
        checkMetricStmt.setString(1, key);
        rs = checkMetricStmt.executeQuery();
        
        while (rs.next()) {
            idMetric = rs.getInt("metric_id");
        }
        
        rs.close();
        
        nameMap.put(key, mapId);
        
        return idMetric;
    }
        

}
    
