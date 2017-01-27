/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.Connection;
import util.BaseDb;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 *
 * @author Enrique
 */
public class CommentDb extends BaseDb {
    BatchedStatement insertStmt = null;
    PreparedStatement checkStmt = null;
    BatchedStatement updateStmt = null;
    
    public enum CheckResult {
        MISSING, DIFFERS, EXISTS
    };
    
    public CommentDb() {
        String sql = "insert into gros.comment(comment_id,issue_id,message,author,date,updater,updated_date) values (?,?,?,?,?,?,?);";
        insertStmt = new BatchedStatement(sql);
        
        sql = "update gros.comment set message=?, author=?, date=?, updater=?, updated_date=? where comment_id=?;";
        updateStmt = new BatchedStatement(sql);
    }
    
    private void getCheckStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkStmt == null) {
            Connection con = insertStmt.getConnection();
            checkStmt = con.prepareStatement("select message, author, date, updater, updated_date from gros.comment where comment_id=?;");
        }
    }
    
    public CheckResult check_comment(int comment_id, String message, String author, Timestamp date, String updater, Timestamp updated_date) throws SQLException, IOException, PropertyVetoException {
        getCheckStmt();
        
        checkStmt.setInt(1, comment_id);
        ResultSet rs = checkStmt.executeQuery();

        CheckResult result = CheckResult.MISSING;
        while (rs.next()) {
            if (message.equals(rs.getString("message")) &&
                    author.equals(rs.getString("author")) &&
                    date.equals(rs.getTimestamp("date")) &&
                    updater.equals(rs.getString("updater")) &&
                    updated_date.equals(rs.getTimestamp("updated_date"))) {
                result = CheckResult.EXISTS;
            }
            else {
                result = CheckResult.DIFFERS;
            }
        }
        
        rs.close();
        
        return result;
    }
    
    public void insert_comment(int comment_id, int issue_id, String message, String author, Timestamp date, String updater, Timestamp updated_date) throws SQLException, IOException, PropertyVetoException{    
        PreparedStatement pstmt = insertStmt.getPreparedStatement();
        pstmt.setInt(1, comment_id);
        pstmt.setInt(2, issue_id);
        pstmt.setString(3, message);
        pstmt.setString(4, author);
        pstmt.setTimestamp(5, date);
        pstmt.setString(6, updater);
        pstmt.setTimestamp(7, updated_date);
                             
        insertStmt.batch();
    }
    
    public void update_comment(int comment_id, String message, String author, Timestamp date, String updater, Timestamp updated_date) throws SQLException, IOException, PropertyVetoException {
        PreparedStatement pstmt = updateStmt.getPreparedStatement();
        pstmt.setString(1, message);
        pstmt.setString(2, author);
        pstmt.setTimestamp(3, date);
        pstmt.setString(4, updater);
        pstmt.setTimestamp(5, updated_date);
        
        pstmt.setInt(6, comment_id);
        
        updateStmt.batch();
    }
    
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
    
