/**
 * VCS review system merge request table.
 * 
 * Copyright 2017-2020 ICTU
 * Copyright 2017-2022 Leiden University
 * Copyright 2017-2023 Leon Helwerda
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * Database access management for the merge requests table.
 * @author Leon Helwerda
 */
public class MergeRequestDb extends BaseDb implements AutoCloseable {
    private BatchedStatement insertStmt = null;
    private PreparedStatement checkStmt = null;
    private BatchedStatement updateStmt = null;
    
    public enum CheckResult {
        MISSING, DIFFERS, EXISTS
    }
    
    public MergeRequestDb() {
        String sql = "insert into gros.merge_request(repo_id,request_id,title,description,status,source_branch,target_branch,author_id,assignee_id,upvotes,downvotes,created_date,updated_date,sprint_id) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
        insertStmt = new BatchedStatement(sql);
        
        sql = "update gros.merge_request set title=?, description=?, status=?, source_branch=?, target_branch=?, author_id=?, assignee_id=?, upvotes=?, downvotes=?, created_date=?, updated_date=?, sprint_id=? WHERE repo_id=? AND request_id=?;";
        updateStmt = new BatchedStatement(sql);
    }
    
    private void getCheckStmt() throws SQLException, PropertyVetoException {
        if (checkStmt == null) {
            Connection con = insertStmt.getConnection();
            checkStmt = con.prepareStatement("select title, description, status, source_branch, target_branch, author_id, assignee_id, upvotes, downvotes, created_date, updated_date from gros.merge_request where repo_id=? AND request_id=?;");
        }
    }
    
    /**
     * Check whether a merge request exists in the database and that it has the same
     * properties as the provided parameters.
     * @param repo_id Idenitifer of the repository the request is made for
     * @param request_id Internal identifier for the merge request
     * @param title The short header message describing what the merge request is about
     * @param description The contents of the request message
     * @param status The status of the request
     * @param source_branch The branch from which commits should be merged
     * @param target_branch The branch at which the commits should be merged into
     * @param author_id Identifier of the VCS developer that started the request
     * @param assignee_id Identifier of the VCS developer that should review the request,
     * or null if nobody is assigned
     * @param upvotes Number of votes from the development team in support of the merge request
     * @param downvotes Number of votes from the development team against the merge request
     * @param created_date Timestamp at which the merge request is created
     * @param updated_date Timestamp at which the merge request received an update
     * @return An indicator of the state of the database regarding the given merge request.
     * This is CheckResult.MISSING if the merge request with the provided repository
     * and request identifiers does not exist. This is CheckResult.DIFFERS if there
     * is a row with the provided repsoitory and merge request identifiers in the database,
     * but it has different values in its fields. This is CheckResult.EXISTS if there
     * is a merge request in the database that matches all the provided parameters.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public CheckResult check_request(int repo_id, int request_id, String title, String description, String status, String source_branch, String target_branch, int author_id, Integer assignee_id, int upvotes, int downvotes, Timestamp created_date, Timestamp updated_date) throws SQLException, PropertyVetoException {
        getCheckStmt();
        
        checkStmt.setInt(1, repo_id);
        checkStmt.setInt(2, request_id);
        CheckResult result;
        try (ResultSet rs = checkStmt.executeQuery()) {
            result = CheckResult.MISSING;
            while (rs.next()) {
                if (title.equals(rs.getString("title")) &&
                        description.equals(rs.getString("description")) &&
                        status.equals(rs.getString("status")) &&
                        source_branch.equals(rs.getString("source_branch")) &&
                        target_branch.equals(rs.getString("target_branch")) &&
                        author_id == rs.getInt("author_id") &&
                        (assignee_id == null ? rs.getObject("assignee_id") == null : assignee_id.equals(rs.getInt("assignee_id"))) &&
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
    
    /**
     * Insert a new merge request in the database.
     * @param repo_id Idenitifer of the repository the request is made for
     * @param request_id Internal identifier for the merge request
     * @param title The short header message describing what the merge request is about
     * @param description The contents of the request message
     * @param status The status of the request
     * @param source_branch The branch from which commits should be merged
     * @param target_branch The branch at which the commits should be merged into
     * @param author_id Identifier of the VCS developer that started the request
     * @param assignee_id Identifier of the VCS developer that should review the request,
     * or null if nobody is assigned
     * @param upvotes Number of votes from the development team in support of the merge request
     * @param downvotes Number of votes from the development team against the merge request
     * @param created_date Timestamp at which the merge request is created
     * @param updated_date Timestamp at which the merge request received an update
     * @param sprint_id The sprint in which the request was created
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_request(int repo_id, int request_id, String title, String description, String status, String source_branch, String target_branch, int author_id, Integer assignee_id, int upvotes, int downvotes, Timestamp created_date, Timestamp updated_date, int sprint_id) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertStmt.getPreparedStatement();
        pstmt.setInt(1, repo_id);
        pstmt.setInt(2, request_id);
        pstmt.setString(3, title);
        pstmt.setString(4, description);
        pstmt.setString(5, status);
        pstmt.setString(6, source_branch);
        pstmt.setString(7, target_branch);
        pstmt.setInt(8, author_id);
        setInteger(pstmt, 9, assignee_id);
        pstmt.setInt(10, upvotes);
        pstmt.setInt(11, downvotes);
        pstmt.setTimestamp(12, created_date);
        pstmt.setTimestamp(13, updated_date);
        pstmt.setInt(14, sprint_id);
                             
        insertStmt.batch();
    }
    
    /**
     * Update an existing merge request in the database with new values.
     * @param repo_id Idenitifer of the repository the request is made for
     * @param request_id Internal identifier for the merge request
     * @param title The short header message describing what the merge request is about
     * @param description The contents of the request message
     * @param status The status of the request
     * @param source_branch The branch from which commits should be merged
     * @param target_branch The branch at which the commits should be merged into
     * @param author_id Identifier of the VCS developer that started the request
     * @param assignee_id Identifier of the VCS developer that should review the request,
     * or null if nobody is assigned
     * @param upvotes Number of votes from the development team in support of the merge request
     * @param downvotes Number of votes from the development team against the merge request
     * @param created_date Timestamp at which the merge request is created
     * @param updated_date Timestamp at which the merge request received an update
     * @param sprint_id The sprint in which the request was created
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void update_request(int repo_id, int request_id, String title, String description, String status, String source_branch, String target_branch, int author_id, Integer assignee_id, int upvotes, int downvotes, Timestamp created_date, Timestamp updated_date, int sprint_id) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = updateStmt.getPreparedStatement();
        pstmt.setString(1, title);
        pstmt.setString(2, description);
        pstmt.setString(3, status);
        pstmt.setString(4, source_branch);
        pstmt.setString(5, target_branch);
        pstmt.setInt(6, author_id);
        setInteger(pstmt, 7, assignee_id);
        pstmt.setInt(8, upvotes);
        pstmt.setInt(9, downvotes);
        pstmt.setTimestamp(10, created_date);
        pstmt.setTimestamp(11, updated_date);
        pstmt.setInt(12, sprint_id);
        
        pstmt.setInt(13, repo_id);
        pstmt.setInt(14, request_id);
        
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
    
