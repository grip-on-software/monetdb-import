/*
 * Copyright 2017-2018 ICTU
 * 
 * Importer of gathered software development metrics for MonetDB
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
        String sql = "insert into gros.merge_request_note(repo_id,request_id,thread_id,note_id,parent_id,author_id,comment,created_date,updated_date) values (?,?,?,?,?,?,?,?,?);";
        insertRequestStmt = new BatchedStatement(sql);
        
        sql = "insert into gros.commit_comment(repo_id,version_id,request_id,thread_id,note_id,parent_id,author_id,comment,file,line,end_line,line_type,created_date,updated_date) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        insertCommitStmt = new BatchedStatement(sql);
    }
    
    private void getCheckRequestStmt() throws SQLException, PropertyVetoException {
        if (checkRequestStmt == null) {
            Connection con = insertRequestStmt.getConnection();
            checkRequestStmt = con.prepareStatement("select note_id from gros.merge_request_note where repo_id=? AND request_id=? AND thread_id=? AND note_id=?;");
        }
    }
    
    /**
     * Check whether a merge request note exists in the database.
     * @param repo_id Identifier of the repository the note is made in
     * @param request_id Identifier of the merge request
     * @param thread_id Identifier of the thread that the note belongs to
     * @param note_id Idenfitier of the note
     * @return Whether a merge request note with the provided ientifiers exists
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public boolean check_request_note(int repo_id, int request_id, int thread_id, int note_id) throws SQLException, PropertyVetoException {
        getCheckRequestStmt();
        
        checkRequestStmt.setInt(1, repo_id);
        checkRequestStmt.setInt(2, request_id);
        checkRequestStmt.setInt(3, thread_id);
        checkRequestStmt.setInt(4, note_id);
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
     * @param thread_id Identifier of the thread that the note belongs in
     * @param note_id Idenfitier of the note that is being added
     * @param parent_id Identifier of another note that this note is a reply to
     * @param dev_id Identifier of the VCS developer that made the note
     * @param comment Plain text comment message of the note
     * @param created_date Timestamp at which the comment is added to the merge request
     * @param updated_date Timestamp at which the comment was most recently edited,
     * or null if the update date is not known
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_request_note(int repo_id, int request_id, int thread_id, int note_id, int parent_id, int dev_id, String comment, Timestamp created_date, Timestamp updated_date) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertRequestStmt.getPreparedStatement();
        pstmt.setInt(1, repo_id);
        pstmt.setInt(2, request_id);
        pstmt.setInt(3, thread_id);
        pstmt.setInt(4, note_id);
        pstmt.setInt(5, parent_id);
        pstmt.setInt(6, dev_id);
        pstmt.setString(7, comment);
        pstmt.setTimestamp(8, created_date);
        setTimestamp(pstmt, 9, updated_date);
                             
        insertRequestStmt.batch();
    }
    
    private void getCheckCommitStmt() throws SQLException, PropertyVetoException {
        if (checkCommitStmt == null) {
            Connection con = insertCommitStmt.getConnection();
            checkCommitStmt = con.prepareStatement("select created_date, updated_date from gros.commit_comment where repo_id=? AND version_id=? AND request_id=? AND thread_id=? AND note_id=? AND parent_id=? AND author_id=? AND comment=? AND file=? AND line=? AND end_line=? AND line_type=? AND created_date=?;");
        }
    }
    
    /**
     * Check whether a commit comment note exists in the database.
     * @param repo_id Identifier of the repository the note is made in
     * @param version_id Commit to which this note is added
     * @param request_id Identifier of the merge request or 0 if not known
     * @param thread_id Identifier of the thread that the note belongs to, or 0
     * @param note_id Idenfitier of the note or 0 if the note has no identifier
     * @param parent_id Identifier of another note that this note is a reply to, or 0
     * @param dev_id Identifier of the VCS developer that made the node
     * @param comment Plain text comment message of the note
     * @param file Path to a file in the repository that is changed in the commit
     * and is discussed by the comment, or null to indicate that the comment
     * belongs to the entire version
     * @param line Line number of the file that is discussed by the comment, or
     * null to indicate that the comment belongs to the entire version
     * @param end_line Line number of end of the range in the file that is discussed
     * by the comment, or null if the comment does not belong to a range
     * @param line_type The type of line being discussed by the comment: 'old'
     * or 'new', or null to indicate that the comment belongs to the entire version
     * @param created_date Timestamp at which the comment is added to the commit,
     * or null if not known
     * @return Whether a commit comment with all the provided properties exists
     * in the database
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public Timestamp check_commit_note(int repo_id, String version_id, int request_id, int thread_id, int note_id, int parent_id, int dev_id, String comment, String file, Integer line, Integer end_line, String line_type, Timestamp created_date) throws SQLException, PropertyVetoException {
        getCheckCommitStmt();
        
        checkCommitStmt.setInt(1, repo_id);
        checkCommitStmt.setString(2, version_id);
        
        checkCommitStmt.setInt(3, request_id);
        checkCommitStmt.setInt(4, thread_id);
        checkCommitStmt.setInt(5, note_id);
        checkCommitStmt.setInt(6, parent_id);
        
        checkCommitStmt.setInt(7, dev_id);
        checkCommitStmt.setString(8, comment);
        
        setString(checkCommitStmt, 9, file);
        setInteger(checkCommitStmt, 10, line);
        setInteger(checkCommitStmt, 11, end_line);
        setString(checkCommitStmt, 12, line_type);
        setTimestamp(checkCommitStmt, 13, created_date);
        try (ResultSet rs = checkCommitStmt.executeQuery()) {
            if (rs.next()) {
                return (rs.getObject("updated_date") == null ? rs.getTimestamp("created_date") : rs.getTimestamp("updated_date"));
            }
        }
        
        return null;
    }
    
    /**
     * Insert a new commit comment note in the database.
     * @param repo_id Identifier of the repository the note is made in
     * @param version_id Commit to which this note is added
     * @param request_id Identifier of the merge request or 0 if not known
     * @param thread_id Identifier of the thread that the note belongs to, or 0
     * @param note_id Idenfitier of the note or 0 if the note has no identifier
     * @param parent_id Identifier of another note that this note is a reply to, or 0
     * @param dev_id Identifier of the VCS developer that made the node
     * @param comment Plain text comment message of the note
     * @param file Path to a file in the repository that is changed in the commit
     * and is discussed by the comment, or null to indicate that the comment
     * belongs to the entire version
     * @param line Line number of the file that is discussed by the comment, or
     * null to indicate that the comment belongs to the entire version
     * @param end_line Line number of end of the range in the file that is discussed
     * by the comment, or null if the comment does not belong to a range
     * @param line_type The type of line being discussed by the comment: 'old'
     * or 'new', or null to indicate that the comment belongs to the entire version
     * @param created_date Timestamp at which the comment is added to the commit,
     * or null if not known
     * @param updated_date Timestamp at which the comment is most recently edited,
     * or null if not known
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_commit_note(int repo_id, String version_id, int request_id, int thread_id, int note_id, int parent_id, int dev_id, String comment, String file, Integer line, Integer end_line, String line_type, Timestamp created_date, Timestamp updated_date) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertCommitStmt.getPreparedStatement();
        pstmt.setInt(1, repo_id);
        pstmt.setString(2, version_id);
        pstmt.setInt(3, request_id);
        pstmt.setInt(4, thread_id);
        pstmt.setInt(5, note_id);
        pstmt.setInt(6, parent_id);
        pstmt.setInt(7, dev_id);
        pstmt.setString(8, comment);
        setString(pstmt, 9, file);
        setInteger(pstmt, 10, line);
        setInteger(pstmt, 11, end_line);
        setString(pstmt, 12, line_type);
        setTimestamp(pstmt, 13, created_date);
        setTimestamp(pstmt, 14, updated_date);
                             
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
    
