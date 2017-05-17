/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import util.BaseDb;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Database access management for comments on merge requests and VCS commits.
 * @author Leon Helwerda
 */
public class NoteDb extends BaseDb implements AutoCloseable {
    private BatchedStatement insertRequestStmt = null;
    private PreparedStatement checkRequestStmt = null;
    private BatchedStatement insertCommitStmt = null;
    private PreparedStatement checkCommitStmt = null;
    
    public enum CheckResult {
        MISSING, DIFFERS, EXISTS
    };
    
    public NoteDb() {
        String sql = "insert into gros.merge_request_note(repo_id,request_id,note_id,developer_id,comment,created_date) values (?,?,?,?,?,?);";
        insertRequestStmt = new BatchedStatement(sql);
        
        sql = "insert into gros.commit_comment(repo_id,version_id,developer_id,comment,file,line,line_type,created_date) values (?,?,?,?,?,?,?,?)";
        insertCommitStmt = new BatchedStatement(sql);
    }
    
    private void getCheckRequestStmt() throws SQLException, PropertyVetoException {
        if (checkRequestStmt == null) {
            Connection con = insertRequestStmt.getConnection();
            checkRequestStmt = con.prepareStatement("select note_id from gros.merge_request_note where repo_id=? AND request_id=? AND note_id=?;");
        }
    }
    
    /**
     * Check whether a merge request note exists in the database.
     * @param repo_id Identifier of the repository the note is made in
     * @param request_id Identifier of the merge request
     * @param note_id Idenfitier of the note
     * @return Whether a merge request note with the provided ientifiers exists
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public boolean check_request_note(int repo_id, int request_id, int note_id) throws SQLException, PropertyVetoException {
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
    
    /**
     * Insert a new merge request note in the database.
     * @param repo_id Identifier of the repository the note is made in
     * @param request_id Identifier of the merge request
     * @param note_id Idenfitier of the note
     * @param dev_id Identifier of the VCS developer that made the note
     * @param comment Plain text comment message of the note
     * @param created_date Timestamp at which the comment is added to the merge request
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_request_note(int repo_id, int request_id, int note_id, int dev_id, String comment, Timestamp created_date) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertRequestStmt.getPreparedStatement();
        pstmt.setInt(1, repo_id);
        pstmt.setInt(2, request_id);
        pstmt.setInt(3, note_id);
        pstmt.setInt(4, dev_id);
        pstmt.setString(5, comment);
        pstmt.setTimestamp(6, created_date);
                             
        insertRequestStmt.batch();
    }
    
    private void getCheckCommitStmt() throws SQLException, PropertyVetoException {
        if (checkCommitStmt == null) {
            Connection con = insertCommitStmt.getConnection();
            checkCommitStmt = con.prepareStatement("select 1 from gros.commit_comment where repo_id=? AND version_id=? AND author=? AND comment=? AND file=? AND line=? AND line_type=? AND created_date=?;");
        }
    }
    
    /**
     * Check whether a commit comment note exists in the database.
     * @param repo_id Identifier of the repository the note is made in
     * @param version_id Commit to which this note is added
     * @param dev_id Identifier of the VCS developer that made the node
     * @param comment Plain text comment message of the note
     * @param file Path to a file in the repository that is changed in the commit
     * and is discussed by the comment, or null to indicate that the comment
     * belongs to the entire version
     * @param line Line number of the file that is discussed by the comment, or
     * null to indicate that the comment belongs to the entire version
     * @param line_type The type of line being discussed by the comment: 'old'
     * or 'new', or null to indicate that the comment belongs to the entire version
     * @param created_date Timestamp at which the comment is added to the commit,
     * or null if not known
     * @return Whether a commit comment with all the provided properties exists
     * in the database
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public boolean check_commit_note(int repo_id, String version_id, int dev_id, String comment, String file, Integer line, String line_type, Timestamp created_date) throws SQLException, PropertyVetoException {
        getCheckCommitStmt();
        
        checkCommitStmt.setInt(1, repo_id);
        checkCommitStmt.setString(2, version_id);
        checkCommitStmt.setInt(3, dev_id);
        checkCommitStmt.setString(4, comment);
        setString(checkCommitStmt, 5, file);
        setInteger(checkCommitStmt, 6, line);
        setString(checkCommitStmt, 7, line_type);
        setTimestamp(checkCommitStmt, 8, created_date);
        try (ResultSet rs = checkCommitStmt.executeQuery()) {
            if (rs.next()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Insert a new commit comment note in the database.
     * @param repo_id Identifier of the repository the note is made in
     * @param version_id Commit to which this note is added
     * @param dev_id Identifier of the VCS developer that made the node
     * @param comment Plain text comment message of the note
     * @param file Path to a file in the repository that is changed in the commit
     * and is discussed by the comment, or null to indicate that the comment
     * belongs to the entire version
     * @param line Line number of the file that is discussed by the comment, or
     * null to indicate that the comment belongs to the entire version
     * @param line_type The type of line being discussed by the comment: 'old'
     * or 'new', or null to indicate that the comment belongs to the entire version
     * @param created_date Timestamp at which the comment is added to the commit,
     * or null if not known
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_commit_note(int repo_id, String version_id, int dev_id, String comment, String file, Integer line, String line_type, Timestamp created_date) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertCommitStmt.getPreparedStatement();
        pstmt.setInt(1, repo_id);
        pstmt.setString(2, version_id);
        pstmt.setInt(3, dev_id);
        pstmt.setString(4, comment);
        setString(pstmt, 5, file);
        setInteger(pstmt, 6, line);
        setString(pstmt, 7, line_type);
        setTimestamp(pstmt, 8, created_date);
                             
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
    
