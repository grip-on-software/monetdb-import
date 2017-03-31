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
public class TagDb  extends BaseDb implements AutoCloseable {
    BatchedStatement insertStmt = null;
    PreparedStatement checkStmt = null;
    BatchedStatement updateStmt = null;
    
    public enum CheckResult {
        MISSING, DIFFERS, EXISTS
    };
    
    public TagDb() {
        String sql = "insert into gros.tag(repo_id,tag_name,version_id,message,tagged_date,tagger_id) values (?,?,?,?,?,?,?);";
        insertStmt = new BatchedStatement(sql);
        
        sql = "update gros.tag set version_id=?, message=?, tagged_date=?, tagger_id=? where repo_id=? and tag_name=?;";
        updateStmt = new BatchedStatement(sql);
    }
    
    private void getCheckStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkStmt == null) {
            Connection con = insertStmt.getConnection();
            checkStmt = con.prepareStatement("select version_id, message, tagged_date, tagger_id from gros.tag where repo_id=? and tag_name=?;");
        }
    }
    
    public CheckResult check_tag(int repo_id, String tag_name, String version_id, String message, Timestamp tagged_date, Integer tagger_id) throws SQLException, IOException, PropertyVetoException {
        getCheckStmt();
        
        checkStmt.setInt(1, repo_id);
        checkStmt.setString(2, tag_name);
        CheckResult result;
        try (ResultSet rs = checkStmt.executeQuery()) {
            result = CheckResult.MISSING;
            while (rs.next()) {
                if (version_id.equals(rs.getInt("version_id")) &&
                        (message == null ? rs.getString("message") == null : message.equals(rs.getString("message"))) &&
                        (tagged_date == null ? rs.getTimestamp("tagged_date") == null : tagged_date.equals(rs.getTimestamp("tagged_date"))) &&
                        (tagger_id == null ? rs.getObject("tagger_id") == null : tagger_id == rs.getInt("tagger_id"))) {
                    result = CheckResult.EXISTS;
                }
                else {
                    result = CheckResult.DIFFERS;
                }
            }
        }
        
        return result;
    }
    
    public void insert_tag(int repo_id, String tag_name, String version_id, String message, Timestamp tagged_date, Integer tagger_id) throws SQLException, IOException, PropertyVetoException {
        PreparedStatement pstmt = insertStmt.getPreparedStatement();
        pstmt.setInt(1, repo_id);
        pstmt.setString(2, tag_name);
        pstmt.setString(3, version_id);
        setString(pstmt, 4, message);
        setTimestamp(pstmt, 5, tagged_date);
        setInteger(pstmt, 6, tagger_id);
                             
        insertStmt.batch();
    }
    
    public void update_tag(int repo_id, String tag_name, String version_id, String message, Timestamp tagged_date, Integer tagger_id) throws SQLException, IOException, PropertyVetoException {
        PreparedStatement pstmt = updateStmt.getPreparedStatement();
        pstmt.setString(1, version_id);
        setString(pstmt, 2, message);
        setTimestamp(pstmt, 3, tagged_date);
        setInteger(pstmt, 4, tagger_id);
        
        pstmt.setInt(5, repo_id);
        pstmt.setString(6, tag_name);
        
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
