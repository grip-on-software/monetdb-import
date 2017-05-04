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
 * @author Leon Helwerda
 */
public class NoteDb extends BaseDb implements AutoCloseable {
    BatchedStatement insertRequestStmt = null;
    PreparedStatement checkRequestStmt = null;
    BatchedStatement insertCommitStmt = null;
    PreparedStatement checkCommitStmt = null;
    
    public enum CheckResult {
        MISSING, DIFFERS, EXISTS
    };
    
    public NoteDb() {
        String sql = "insert into gros.merge_request_note(repo_id,request_id,note_id,developer_id,comment,created_date) values (?,?,?,?,?,?);";
        insertRequestStmt = new BatchedStatement(sql);
        
        sql = "insert into gros.commit_comment(repo_id,version_id,developer_id,comment,file,line,line_type) values (?,?,?,?,?,?,?)";
        insertCommitStmt = new BatchedStatement(sql);
    }
    
    private void getCheckRequestStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkRequestStmt == null) {
            Connection con = insertRequestStmt.getConnection();
            checkRequestStmt = con.prepareStatement("select note_id from gros.merge_request_note where repo_id=? AND request_id=? AND note_id=?;");
        }
    }
    
    public boolean check_request_note(int repo_id, int request_id, int note_id) throws SQLException, IOException, PropertyVetoException {
        getCheckRequestStmt();
        
        checkRequestStmt.setInt(1, repo_id);
        checkRequestStmt.setInt(2, request_id);
        checkRequestStmt.setInt(3, note_id);
        try (ResultSet rs = checkRequestStmt.executeQuery()) {
            if (rs.next()) {
                return true;
            }
        }
        
        return false;
    }
    
    public void insert_request_note(int repo_id, int request_id, int note_id, int dev_id, String comment, Timestamp created_date) throws SQLException, IOException, PropertyVetoException {
        PreparedStatement pstmt = insertRequestStmt.getPreparedStatement();
        pstmt.setInt(1, repo_id);
        pstmt.setInt(2, request_id);
        pstmt.setInt(3, note_id);
        pstmt.setInt(4, dev_id);
        pstmt.setString(5, comment);
        pstmt.setTimestamp(6, created_date);
                             
        insertRequestStmt.batch();
    }
    
    private void getCheckCommitStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkCommitStmt == null) {
            Connection con = insertCommitStmt.getConnection();
            checkCommitStmt = con.prepareStatement("select 1 from gros.commit_comment where repo_id=? AND version_id=? AND author=? AND comment=? AND file=? AND line=? AND line_type=? AND encryption=?;");
        }
    }
    
    public boolean check_commit_note(int repo_id, String version_id, int dev_id, String comment, String file, Integer line, String line_type) throws SQLException, IOException, PropertyVetoException {
        getCheckCommitStmt();
        
        checkCommitStmt.setInt(1, repo_id);
        checkCommitStmt.setString(2, version_id);
        checkCommitStmt.setInt(3, dev_id);
        checkCommitStmt.setString(4, comment);
        setString(checkCommitStmt, 5, file);
        setInteger(checkCommitStmt, 6, line);
        setString(checkCommitStmt, 7, line_type);
        try (ResultSet rs = checkCommitStmt.executeQuery()) {
            if (rs.next()) {
                return true;
            }
        }
        
        return false;
    }
    
    public void insert_commit_note(int repo_id, String version_id, int dev_id, String comment, String file, Integer line, String line_type) throws SQLException, IOException, PropertyVetoException {
        PreparedStatement pstmt = insertCommitStmt.getPreparedStatement();
        pstmt.setInt(1, repo_id);
        pstmt.setString(2, version_id);
        pstmt.setInt(3, dev_id);
        pstmt.setString(4, comment);
        setString(pstmt, 5, file);
        setInteger(pstmt, 6, line);
        setString(pstmt, 7, line_type);
                             
        insertCommitStmt.batch();
    }
        
    @Override
    public void close() throws SQLException {
        insertRequestStmt.execute();
        insertRequestStmt.close();
        
        if (checkRequestStmt != null) {
            checkRequestStmt.close();
            checkRequestStmt = null;
        }

        insertCommitStmt.execute();
        insertCommitStmt.close();
        
        if (checkCommitStmt != null) {
            checkCommitStmt.close();
            checkCommitStmt = null;
        }
    }

}
    
