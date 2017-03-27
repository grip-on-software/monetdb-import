/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import util.BaseDb;

/**
 *
 * @author leonhelwerda
 */
public class FixVersionDb extends BaseDb implements AutoCloseable {
    BatchedStatement insertStmt = null;
    PreparedStatement checkStmt = null;
    BatchedStatement updateStmt = null;

    public enum CheckResult {
        MISSING, DIFFERS, EXISTS
    };

    public FixVersionDb() {
        String sql = "insert into gros.fixversion(id,project_id,name,description,start_date,release_date,released) values (?,?,?,?,?,?,?);";
        insertStmt = new BatchedStatement(sql);
        
        sql = "update gros.fixversion set name=?, description=? start_date=?, release_date=?, released=? where id=? and project_id=?;";
        updateStmt = new BatchedStatement(sql);
    }
    
    private void getCheckStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkStmt == null) {
            Connection con = insertStmt.getConnection();
            checkStmt = con.prepareStatement("select name, description, start_date, release_date, released from gros.fixversion where id=? and project_id=?;");
        }
    }
    
    private boolean compareDates(Date date, Date current_date) {
        if (date == null) {
            return (current_date == null);
        }
        return date.equals(current_date);
    }
    
    public CheckResult check_version(int id, int project_id, String name, String description, Date start_date, Date release_date, boolean released) throws SQLException, IOException, PropertyVetoException {
        getCheckStmt();
        
        checkStmt.setInt(1, id);
        checkStmt.setInt(2, project_id);
        CheckResult result;
        try (ResultSet rs = checkStmt.executeQuery()) {
            result = CheckResult.MISSING;
            while (rs.next()) {
                if (name.equals(rs.getString("name")) &&
                        description.equals(rs.getString("description")) &&
                        compareDates(start_date, rs.getDate("start_date")) &&
                        compareDates(release_date, rs.getDate("release_date")) &&
                        released == rs.getBoolean("released")) {
                    result = CheckResult.EXISTS;
                }
                else {
                    result = CheckResult.DIFFERS;
                }
            }
        }
        
        return result;
    }
    
    public void insert_version(int id, int project_id, String name, String description, Date start_date, Date release_date, boolean released) throws SQLException, IOException, PropertyVetoException{    
        PreparedStatement pstmt = insertStmt.getPreparedStatement();
        pstmt.setInt(1, id);
        pstmt.setInt(2, project_id);
        pstmt.setString(3, name);
        pstmt.setString(4, description);
        setDate(pstmt, 5, start_date);
        setDate(pstmt, 6, release_date);
        pstmt.setBoolean(7, released);
                             
        insertStmt.batch();
    }
    
    public void update_version(int id, int project_id, String name, String description, Date start_date, Date release_date, boolean released) throws SQLException, IOException, PropertyVetoException {
        PreparedStatement pstmt = updateStmt.getPreparedStatement();
        pstmt.setString(1, name);
        pstmt.setString(2, description);
        setDate(pstmt, 3, start_date);
        setDate(pstmt, 4, release_date);
        pstmt.setBoolean(5, released);
        
        pstmt.setInt(6, id);
        pstmt.setInt(7, project_id);
        
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
