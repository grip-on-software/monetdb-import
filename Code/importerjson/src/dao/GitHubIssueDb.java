/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import util.BaseDb;

/**
 * Database access management for the GitHub issue table.
 * @author Leon Helwerda
 */
public class GitHubIssueDb extends BaseDb implements AutoCloseable {
    private BatchedStatement insertIssueStmt = null;
    private PreparedStatement checkIssueStmt = null;
    private BatchedStatement updateIssueStmt = null;
    
    public enum CheckResult {
        MISSING, DIFFERS, EXISTS
    };
    
    public GitHubIssueDb() {
        String sql = "insert into gros.github_issue(repo_id,issue_id,title,description,status,author_id,assignee_id,created_date,updated_date,pull_request_id,labels,closed_date,closer_id) values (?,?,?,?,?,?,?,?,?,?,?,?,?);";
        insertIssueStmt = new BatchedStatement(sql);
        
        sql = "update gros.github_issue set title=?, description=?, status=?, author_id=?, assignee_id=?, created_date=?, updated_date=?, pull_request_id=?, labels=?, closed_date=?, closer_id=? WHERE repo_id=? AND issue_id=?;";
        updateIssueStmt = new BatchedStatement(sql);
    }

    private void getCheckIssueStmt() throws SQLException, PropertyVetoException {
        if (checkIssueStmt == null) {
            Connection con = insertIssueStmt.getConnection();
            checkIssueStmt = con.prepareStatement("select title, description, status, author_id, assignee_id, created_date, updated_date, pull_request_id, labels, closed_date, closer_id from gros.github_issue where repo_id=? AND issue_id=?;");
        }
    }
    
    /**
     * Check whether a GitHub issue exists in the database and that it has the
     * same properties as the provided parameters.
     * @param repo_id Idenitifer of the repository the issue is made for
     * @param issue_id Internal identifier for the issue
     * @param title The short header message describing what the issue is about
     * @param description The contents of the issue message
     * @param status The status of the issue
     * @param author_id Identifier of the VCS developer that started the issue
     * @param assignee_id Identifier of the VCS developer that should pick up
     * the issue, or null if nobody is assigned
     * @param created_date Timestamp at which the issue is created
     * @param updated_date Timestamp at which the issue received an update
     * @param pull_request_id Pull request that is linked to this issue, or null
     * if no such link exists
     * @param labels The number of labels added to the issue
     * @param closer_id Identifier of the VCS developer that closed the issue,
     * or null if the issue has not been closed
     * @param closed_date Timestamp at which the issue is closed, or null if the
     * issue has not been closed
     * @return An indicator of the state of the database regarding the given issue.
     * This is CheckResult.MISSING if the issue with the provided repository
     * and issue identifiers does not exist. This is CheckResult.DIFFERS if there
     * is a row with the provided repsoitory and issue identifiers in the database,
     * but it has different values in its fields. This is CheckResult.EXISTS if there
     * is an issue in the database that matches all the provided parameters.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public CheckResult check_issue(int repo_id, int issue_id, String title, String description, String status, int author_id, Integer assignee_id, Timestamp created_date, Timestamp updated_date, Integer pull_request_id, int labels, Timestamp closed_date, Integer closer_id) throws SQLException, PropertyVetoException {
        getCheckIssueStmt();
        
        checkIssueStmt.setInt(1, repo_id);
        checkIssueStmt.setInt(2, issue_id);
        CheckResult result;
        try (ResultSet rs = checkIssueStmt.executeQuery()) {
            result = CheckResult.MISSING;
            while (rs.next()) {
                if (title.equals(rs.getString("title")) &&
                        description.equals(rs.getString("description")) &&
                        status.equals(rs.getString("status")) &&
                        author_id == rs.getInt("author_id") &&
                        (assignee_id == null ? rs.getObject("assignee_id") == null : assignee_id == rs.getInt("assignee_id")) &&
                        created_date.equals(rs.getTimestamp("created_date")) &&
                        updated_date.equals(rs.getTimestamp("updated_date")) &&
                        (pull_request_id == null ? rs.getObject("pull_request_id") == null : pull_request_id == rs.getInt("pull_request_id")) &&
                        labels == rs.getInt("labels") &&
                        (closed_date == null ? rs.getObject("closed_date") == null : closed_date.equals(rs.getTimestamp("closed_date"))) &&
                        (closer_id == null ? rs.getObject("closer_id") == null : closer_id == rs.getInt("closer_id"))) {
                    result = CheckResult.EXISTS;
                }
                else {
                    result = CheckResult.DIFFERS;
                }
            }
        }
        
        return result;
    }
    
    /**
     * Insert a new GitHub issue in the database.
     * @param repo_id Idenitifer of the repository the issue is made for
     * @param issue_id Internal identifier for the issue
     * @param title The short header message describing what the issue is about
     * @param description The contents of the issue message
     * @param status The status of the issue
     * @param author_id Identifier of the VCS developer that started the issue
     * @param assignee_id Identifier of the VCS developer that should pick up
     * the issue, or null if nobody is assigned
     * @param created_date Timestamp at which the issue is created
     * @param updated_date Timestamp at which the issue received an update
     * @param pull_request_id Pull request that is linked to this issue, or null
     * if no such link exists
     * @param labels The number of labels added to the issue
     * @param closer_id Identifier of the VCS developer that closed the issue,
     * or null if the issue has not been closed
     * @param closed_date Timestamp at which the issue is closed, or null if the
     * issue has not been closed
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_issue(int repo_id, int issue_id, String title, String description, String status, int author_id, Integer assignee_id, Timestamp created_date, Timestamp updated_date, Integer pull_request_id, int labels, Timestamp closed_date, Integer closer_id) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertIssueStmt.getPreparedStatement();
        pstmt.setInt(1, repo_id);
        pstmt.setInt(2, issue_id);
        pstmt.setString(3, title);
        pstmt.setString(4, description);
        pstmt.setString(5, status);
        pstmt.setInt(6, author_id);
        setInteger(pstmt, 7, assignee_id);
        pstmt.setTimestamp(8, created_date);
        pstmt.setTimestamp(9, updated_date);
        setInteger(pstmt, 10, pull_request_id);
        pstmt.setInt(11, labels);
        setTimestamp(pstmt, 12, closed_date);
        setInteger(pstmt, 13, closer_id);
                             
        insertIssueStmt.batch();
    }
    
    /**
     * Update an existing GitHub issue in the database with new values.
     * @param repo_id Idenitifer of the repository the issue is made for
     * @param issue_id Internal identifier for the issue
     * @param title The short header message describing what the issue is about
     * @param description The contents of the issue message
     * @param status The status of the issue
     * @param author_id Identifier of the VCS developer that started the issue
     * @param assignee_id Identifier of the VCS developer that should pick up
     * the issue, or null if nobody is assigned
     * @param created_date Timestamp at which the issue is created
     * @param updated_date Timestamp at which the issue received an update
     * @param pull_request_id Pull request that is linked to this issue, or null
     * if no such link exists
     * @param labels The number of labels added to the issue
     * @param closer_id Identifier of the VCS developer that closed the issue,
     * or null if the issue has not been closed
     * @param closed_date Timestamp at which the issue is closed, or null if the
     * issue has not been closed
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void update_issue(int repo_id, int issue_id, String title, String description, String status, int author_id, Integer assignee_id, Timestamp created_date, Timestamp updated_date, Integer pull_request_id, int labels, Timestamp closed_date, Integer closer_id) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = updateIssueStmt.getPreparedStatement();
        pstmt.setString(1, title);
        pstmt.setString(2, description);
        pstmt.setString(3, status);
        pstmt.setInt(4, author_id);
        setInteger(pstmt, 5, assignee_id);
        pstmt.setTimestamp(6, created_date);
        pstmt.setTimestamp(7, updated_date);
        setInteger(pstmt, 8, pull_request_id);
        pstmt.setInt(9, labels);
        setTimestamp(pstmt, 10, closed_date);
        setInteger(pstmt, 11, closer_id);
        
        pstmt.setInt(12, repo_id);
        pstmt.setInt(13, issue_id);
        
        updateIssueStmt.batch();
    }
    
    @Override
    public void close() throws SQLException {
        insertIssueStmt.execute();
        insertIssueStmt.close();
        
        updateIssueStmt.execute();
        updateIssueStmt.close();
        
        if (checkIssueStmt != null) {
            checkIssueStmt.close();
            checkIssueStmt = null;
        }
    }

}