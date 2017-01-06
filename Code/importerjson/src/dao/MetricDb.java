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
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrique
 */
public class MetricDb extends BaseImport{
    PreparedStatement insertMetricValueStmt = null;
    
    public void insert_metric(String name){
        
        Connection con = null;
        Statement st = null;
        String sql="";
    
        try {
            con = DataSource.getInstance().getConnection();
       
            st = con.createStatement();
            sql = "insert into gros.metric(name) values ('"+name+"');";
                    
            st.executeUpdate(sql);
     
        } catch (SQLException | IOException | PropertyVetoException ex) {
            Logger.getLogger(MetricDb.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (st != null) try { st.close(); } catch (SQLException e) {e.printStackTrace();}
            if (con != null) try { con.close(); } catch (SQLException e) {e.printStackTrace();}
        }
        
    
    }
    
    public void insert_metricValue(int id, Integer value, String category, String date, int project){
        
        Connection con = null;
        Statement st = null;
        String sql="";
    
        try {
            if (insertMetricValueStmt != null) {
                con = DataSource.getInstance().getConnection();
                sql = "insert into gros.metric_value values (?,?,?,?,?);";
                insertMetricValueStmt = con.prepareStatement(sql);
            }
            
            insertMetricValueStmt.setInt(1, id);
            insertMetricValueStmt.setInt(2, value);
            insertMetricValueStmt.setString(3, category);
            insertMetricValueStmt.setTimestamp(4, Timestamp.valueOf(date));
            insertMetricValueStmt.setInt(5, project);
                    
            insertMetricValueStmt.addBatch();

        } catch (SQLException | IOException | PropertyVetoException ex) {
            Logger.getLogger(MetricDb.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (con != null) try { con.close(); } catch (SQLException e) {e.printStackTrace();}
        }
        
    
    }
    
    public void execute_metricValue() {
        if (insertMetricValueStmt != null) {
            try {
                insertMetricValueStmt.executeBatch();
            } catch (SQLException ex) {
                Logger.getLogger(MetricDb.class.getName()).log(Level.SEVERE, null, ex);
            }
            finally {
               try {
                   insertMetricValueStmt.close();
               } catch (SQLException e) {e.printStackTrace();}
               insertMetricValueStmt = null;
            }
        }
    }
   
    public int check_metric(String name){

        int idMetric = 0;
        Connection con = null;
        Statement st = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            con = DataSource.getInstance().getConnection();
            
            st = con.createStatement();
            String sql_var = "SELECT metric_id FROM gros.metric WHERE UPPER(name) = '" + name.toUpperCase().trim()+ "'";
            rs = st.executeQuery(sql_var);
 
            while (rs.next()) {
                idMetric = rs.getInt("metric_id");
            }

            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {e.printStackTrace();}
            if (st != null) try { st.close(); } catch (SQLException e) {e.printStackTrace();}
            if (con != null) try { con.close(); } catch (SQLException e) {e.printStackTrace();}
        }
        
        return idMetric;
    }
        

}
    
