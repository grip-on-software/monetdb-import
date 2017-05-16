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
public class MergeRequestDb extends BaseDb implements AutoCloseable {
    BatchedStatement insertStmt = null;
    PreparedStatement checkStmt = null;
    BatchedStatement updateStmt = null;
    
    public enum CheckResult {
        MISSING, DIFFERS, EXISTS
    };
    
    public MergeRequestDb() {
        String sql = "insert into gros.merge_request(repo_id,request_id,title,description,source_branch,target_branch,author_id,assignee_id,upvotes,downvotes,created_date,updated_date,sprint_id) values (?,?,?,?,?,?,?,?,?,?,?,?,?);";
        insertStmt = new BatchedStatement(sql);
        
        sql = "update gros.merge_request set title=?, description=?, source_branch=?, target_branch=?, author_id=?, assignee_id=?, upvotes=?, downvotes=?, created_date=?, updated_date=?, sprint_id=? WHERE repo_id=? AND request_id=?;";
        updateStmt = new BatchedStatement(sql);
    }
    
    private void getCheckStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkStmt == null) {
            Connection con = insertStmt.getConnection();
            checkStmt = con.prepareStatement("select title, description, source_branch, target_branch, author_id, assignee_id, upvotes, downvotes, created_date, updated_date from gros.merge_request where repo_id=? AND request_id=?;");
        }
    }
    
    public CheckResult check_request(int repo_id, int request_id, String title, String description, String source_branch, String target_branch, int author_id, Integer assignee_id, int upvotes, int downvotes, Timestamp created_date, Timestamp updated_date) throws SQLException, IOException, PropertyVetoException {
        getCheckStmt();
        
        checkStmt.setInt(1, repo_id);
        checkStmt.setInt(2, request_id);
        CheckResult result;
        try (ResultSet rs = checkStmt.executeQuery()) {
            result = CheckResult.MISSING;
            while (rs.next()) {
                if (title.equals(rs.getString("title")) &&
                        description.equals(rs.getString("description")) &&
                        source_branch.equals(rs.getString("source_branch")) &&
                        target_branch.equals(rs.getString("target_branch")) &&
                        author_id == rs.getInt("author_id") &&
                        (assignee_id == null ? rs.getObject("assignee_id") == null : assignee_id == rs.getInt("assignee_id")) &&
                        upvotes == rs.getInt("upvotes") &&
                        downvotes == rs.getInt("downvotes") &&
                        created_date.equals(rs.getTimestamp("created_date")) &&
                        updated_date.equals(rs.getTimestamp("updated_date"))) {
                    result = CheckResult.EXISTS;
                }
                else {
                    result = CheckResult.DIFFERS;
                }
            }
        }
        
        return result;
    }
    
    public void insert_request(int repo_id, int request_id, String title, String description, String source_branch, String target_branch, int author_id, Integer assignee_id, int upvotes, int downvotes, Timestamp created_date, Timestamp updated_date, int sprint_id) throws SQLException, IOException, PropertyVetoException {
        PreparedStatement pstmt = insertStmt.getPreparedStatement();
        pstmt.setInt(1, repo_id);
        pstmt.setInt(2, request_id);
        pstmt.setString(3, title);
        pstmt.setString(4, description);
        pstmt.setString(5, source_branch);
        pstmt.setString(6, target_branch);
        pstmt.setInt(7, author_id);
        setInteger(pstmt, 8, assignee_id);
        pstmt.setInt(9, upvotes);
        pstmt.setInt(10, downvotes);
        pstmt.setTimestamp(11, created_date);
        pstmt.setTimestamp(12, updated_date);
        pstmt.setInt(13, sprint_id);
                             
        insertStmt.batch();
    }
    
    public void update_request(int repo_id, int request_id, String title, String description, String source_branch, String target_branch, int author_id, Integer assignee_id, int upvotes, int downvotes, Timestamp created_date, Timestamp updated_date, int sprint_id) throws SQLException, IOException, PropertyVetoException {
        PreparedStatement pstmt = updateStmt.getPreparedStatement();
        pstmt.setString(1, title);
        pstmt.setString(2, description);
        pstmt.setString(3, source_branch);
        pstmt.setString(4, target_branch);
        pstmt.setInt(5, author_id);
        setInteger(pstmt, 6, assignee_id);
        pstmt.setInt(7, upvotes);
        pstmt.setInt(8, downvotes);
        pstmt.setTimestamp(9, created_date);
        pstmt.setTimestamp(10, updated_date);
        pstmt.setInt(11, sprint_id);
        
        pstmt.setInt(12, repo_id);
        pstmt.setInt(13, request_id);
        
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
    
