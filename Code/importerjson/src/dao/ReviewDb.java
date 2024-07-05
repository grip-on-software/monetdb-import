/**
 * TFS pull request review table.
 * 
 * Copyright 2017-2020 ICTU
 * Copyright 2017-2022 Leiden University
 * Copyright 2017-2024 Leon Helwerda
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import util.BaseDb;

/**
 * Database access management for Team Foundation Server pull request reviews.
 * @author leonhelwerda
 */
public class ReviewDb extends BaseDb implements AutoCloseable {
    private BatchedStatement insertStmt = null;
    private BatchedStatement updateStmt = null;
    private PreparedStatement checkStmt = null;
    
    public ReviewDb() {
        insertStmt = new BatchedStatement("insert into gros.merge_request_review(repo_id,request_id,reviewer_id,vote) values (?,?,?,?)");
        updateStmt = new BatchedStatement("update gros.merge_request_review set vote=? where repo_id=? and request_id=? and reviewer_id=?");
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
    
    private void getCheckStmt() throws SQLException, PropertyVetoException {
        if (checkStmt == null) {
            Connection con = insertStmt.getConnection();
            checkStmt = con.prepareStatement("select vote from gros.merge_request_review where repo_id=? and request_id=? and reviewer_id=?");
        }
    }

    /**
     * Check whether a review exists in the table.
     * @param repo_id Identifier of the repository on which the review is made
     * @param request_id Identifier of the pull request on which the review is made
     * @param dev_id Identifier of the VCS developer who made the review
     * @return If the review with the provided identifiers can be found, then
     * this returns the current vote count for the review. Otherwise, null is
     * returned.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public Integer check_review(int repo_id, int request_id, int dev_id) throws SQLException, PropertyVetoException {
        getCheckStmt();
        
        checkStmt.setInt(1, repo_id);
        checkStmt.setInt(2, request_id);
        checkStmt.setInt(3, dev_id);
        
        try (ResultSet rs = checkStmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("vote");
            }
        }
        
        return null;
    }

    /**
     * Insert a new review in the table.
     * @param repo_id Identifier of the repository on which the review is made
     * @param request_id Identifier of the pull request on which the review is made
     * @param dev_id Identifier of the VCS developer who made the review
     * @param vote The vote of the developer
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_review(int repo_id, int request_id, int dev_id, int vote) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertStmt.getPreparedStatement();
        
        pstmt.setInt(1, repo_id);
        pstmt.setInt(2, request_id);
        pstmt.setInt(3, dev_id);
        pstmt.setInt(4, vote);
        
        insertStmt.batch();
    }

    /**
     * Update an existing review in the table.
     * @param repo_id Identifier of the repository on which the review is made
     * @param request_id Identifier of the pull request on which the review is made
     * @param dev_id Identifier of the VCS developer who made the review
     * @param vote The vote of the developer
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void update_review(int repo_id, int request_id, int dev_id, int vote) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = updateStmt.getPreparedStatement();
        
        pstmt.setInt(1, vote);
        
        pstmt.setInt(2, repo_id);
        pstmt.setInt(3, request_id);
        pstmt.setInt(4, dev_id);
        
        updateStmt.batch();
    }
    
}
