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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import util.BaseDb;

/**
 *
 * @author Leon Helwerda
 */
public class SprintDb extends BaseDb implements AutoCloseable {
    BatchedStatement insertStmt = null;
    PreparedStatement checkStmt = null;
    BatchedStatement updateStmt = null;

    public enum CheckResult {
        MISSING, DIFFERS, EXISTS
    };

    public SprintDb() {
        String sql = "insert into gros.sprint(sprint_id,project_id,name,start_date,end_date) values (?,?,?,?,?);";
        insertStmt = new BatchedStatement(sql);
        
        sql = "update gros.sprint set name=?, start_date=?, end_date=? where sprint_id=? and project_id=?;";
        updateStmt = new BatchedStatement(sql);
    }
    
    private void getCheckStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkStmt == null) {
            Connection con = insertStmt.getConnection();
            checkStmt = con.prepareStatement("select name, start_date, end_date from gros.sprint where sprint_id=? and project_id=?;");
        }
    }
    
    private boolean compareTimestamps(Timestamp date, Timestamp current_date) {
        if (date == null) {
            return (current_date == null);
        }
        return date.equals(current_date);
    }
    
    public CheckResult check_sprint(int sprint_id, int project_id, String name, Timestamp start_date, Timestamp end_date) throws SQLException, IOException, PropertyVetoException {
        getCheckStmt();
        
        checkStmt.setInt(1, sprint_id);
        checkStmt.setInt(2, project_id);
        CheckResult result;
        try (ResultSet rs = checkStmt.executeQuery()) {
            result = CheckResult.MISSING;
            while (rs.next()) {
                if (name.equals(rs.getString("name")) &&
                        compareTimestamps(start_date, rs.getTimestamp("start_date")) &&
                        compareTimestamps(end_date, rs.getTimestamp("end_date"))) {
                    result = CheckResult.EXISTS;
                }
                else {
                    result = CheckResult.DIFFERS;
                }
            }
        }
        
        return result;
    }
    
    public void insert_sprint(int sprint_id, int project_id, String name, Timestamp start_date, Timestamp end_date) throws SQLException, IOException, PropertyVetoException{    
        PreparedStatement pstmt = insertStmt.getPreparedStatement();
        pstmt.setInt(1, sprint_id);
        pstmt.setInt(2, project_id);
        pstmt.setString(3, name);
        pstmt.setTimestamp(4, start_date);
        pstmt.setTimestamp(5, end_date);
                             
        insertStmt.batch();
    }
    
    public void update_sprint(int sprint_id, int project_id, String name, Timestamp start_date, Timestamp end_date) throws SQLException, IOException, PropertyVetoException {
        PreparedStatement pstmt = updateStmt.getPreparedStatement();
        pstmt.setString(1, name);
        pstmt.setTimestamp(2, start_date);
        pstmt.setTimestamp(3, end_date);
        
        pstmt.setInt(4, sprint_id);
        pstmt.setInt(5, project_id);
        
        updateStmt.batch();
    }
    
    @Override
    public void close() throws SQLException {
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
