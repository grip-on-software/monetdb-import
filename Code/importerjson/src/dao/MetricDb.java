/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import java.beans.PropertyVetoException;
import java.io.IOException;
import util.BaseImport;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 *
 * @author Enrique
 */
public class MetricDb extends BaseImport{
    PreparedStatement checkMetricStmt = null;
    BatchedStatement insertMetricStmt = null;
    BatchedStatement insertMetricValueStmt = null;
    
    public MetricDb() {
        String sql = "insert into gros.metric(name) values (?);";
        insertMetricStmt = new BatchedStatement(sql);
        sql = "insert into gros.metric_value values (?,?,?,?,?);";
        insertMetricValueStmt = new BatchedStatement(sql);
    }
    
    public void insert_metric(String name) throws SQLException, IOException, PropertyVetoException{
        PreparedStatement pstmt = insertMetricStmt.getPreparedStatement();
        
        pstmt.setString(1, name);
        pstmt.addBatch();
        
        // Insert immediately
        insertMetricStmt.execute();
    }
    
    public void insert_metricValue(int id, Integer value, String category, String date, int project) throws SQLException, IOException, PropertyVetoException{
        PreparedStatement pstmt = insertMetricValueStmt.getPreparedStatement();
        
        pstmt.setInt(1, id);
        pstmt.setInt(2, value);
        pstmt.setString(3, category);
        pstmt.setTimestamp(4, Timestamp.valueOf(date));
        pstmt.setInt(5, project);
                    
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
    }
    
    private void getCheckMetricStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkMetricStmt == null) {
            Connection con = insertMetricStmt.getConnection();
            String sql = "SELECT metric_id FROM gros.metric WHERE UPPER(name) = ?";
            checkMetricStmt = con.prepareStatement(sql);
        }
    }
    
    public int check_metric(String name) throws SQLException, IOException, PropertyVetoException {

        int idMetric = 0;
        getCheckMetricStmt();
        ResultSet rs = null;
        
        checkMetricStmt.setString(1, name.toUpperCase().trim());
        rs = checkMetricStmt.executeQuery();
        
        while (rs.next()) {
            idMetric = rs.getInt("metric_id");
        }
        
        return idMetric;
    }
        

}
    
