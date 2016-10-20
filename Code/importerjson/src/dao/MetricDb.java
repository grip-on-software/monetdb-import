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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrique
 */
public class MetricDb extends BaseImport{
    
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
        }
        
    
    }
    
    public void insert_metricValue(int id, String value, String date, int project){
        
        Connection con = null;
        Statement st = null;
        String sql="";
    
        try {
            con = DataSource.getInstance().getConnection();       
            st = con.createStatement();
            sql = "insert into gros.metric_value values ("+id+", '"+value+"','"+date+"'"+","+project+");";
                    
            st.executeUpdate(sql);

        } catch (SQLException | IOException | PropertyVetoException ex) {
            Logger.getLogger(MetricDb.class.getName()).log(Level.SEVERE, null, ex);
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
        }
        
        return idMetric;
    }
        

}
    
