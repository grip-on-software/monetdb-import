/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 * @author Leon Helwerda
 */
public class BatchedStatement {
    Connection con = null;
    PreparedStatement pstmt = null;
    String query = "";
    Integer batchSize;
    Integer maxBatchSize = 1000;
    
    public BatchedStatement(String sql) {
        query = sql;
        batchSize = 0;
    }
    
    public Connection getConnection() throws SQLException, IOException, PropertyVetoException {
        if (con == null) {
            con = DataSource.getInstance().getConnection();
        }
        return con;
    }

    public PreparedStatement getPreparedStatement() throws SQLException, IOException, PropertyVetoException {
        getConnection();
        if (pstmt == null) {
            pstmt = con.prepareStatement(query);
            batchSize = 0;
        }
        return pstmt;
    }
    
    public void batch() throws SQLException {
        if (pstmt != null) {
            pstmt.addBatch();
            batchSize++;
            if (batchSize >= maxBatchSize) {
                execute();
            }
        }
    }
    
    public void execute() throws SQLException {
        if (pstmt != null) {
            pstmt.executeBatch();
            pstmt.clearBatch();
            batchSize = 0;
        }
    }

    public void close() {
        if (con != null) try { con.close(); } catch (SQLException e) {e.printStackTrace();}
        if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {e.printStackTrace();}
        con = null;
        pstmt = null;
        batchSize = 0;
    }
}
