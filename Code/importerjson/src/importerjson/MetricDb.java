/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importerjson;

import java.sql.Connection;
import java.sql.DriverManager;
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
public class MetricDb {
    
    public void insert_metric(String name){
        
        Connection con = null;
        Statement st = null;
        String sql="";
    
        try {
            Class.forName("nl.cwi.monetdb.jdbc.MonetDriver");
            con = DriverManager.getConnection("jdbc:monetdb://localhost:50000/gros", "monetdb", "monetdb");        
            st = con.createStatement();
            sql = "insert into gros.metric(name) values ('"+name+"');";
                    
            st.executeUpdate(sql);
            
            con.close();
            
        } catch (SQLException ex) {
            Logger.getLogger(MetricDb.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(MetricDb.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    
    }
    
    public void insert_metricValue(int id, String value, String date, int project){
        
        Connection con = null;
        Statement st = null;
        String sql="";
    
        try {
            Class.forName("nl.cwi.monetdb.jdbc.MonetDriver");
            con = DriverManager.getConnection("jdbc:monetdb://localhost:50000/gros", "monetdb", "monetdb");        
            st = con.createStatement();
            sql = "insert into gros.metric_value values ("+id+", '"+value+"','"+date+"'"+","+project+");";
                    
            st.executeUpdate(sql);
            
            con.close();
            
        } catch (SQLException ex) {
            Logger.getLogger(MetricDb.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
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

            Class.forName("nl.cwi.monetdb.jdbc.MonetDriver");
            con = DriverManager.getConnection("jdbc:monetdb://localhost:50000/gros", "monetdb", "monetdb");
            
            st = con.createStatement();
            String sql_var = "SELECT metric_id FROM gros.metric WHERE UPPER(name) = '" + name.toUpperCase().trim()+ "'";
            rs = st.executeQuery(sql_var);
 
            while (rs.next()) {
                idMetric = rs.getInt("metric_id");
            }
            
            con.close();
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
        return idMetric;
    }
        

}
    
