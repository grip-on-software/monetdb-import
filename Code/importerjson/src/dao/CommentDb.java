/**
 * JIRA comment table.
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
import util.BaseDb;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Database access management for the JIRA comment table.
 * @author Enrique
 */
public class CommentDb extends BaseDb implements AutoCloseable {
    private BatchedStatement insertStmt = null;
    private PreparedStatement checkStmt = null;
    private BatchedStatement updateStmt = null;
    
    public enum CheckResult {
        MISSING, DIFFERS, EXISTS
    }
    
    public CommentDb() {
        String sql = "insert into gros.comment(comment_id,issue_id,message,author,date,updater,updated_date) values (?,?,?,?,?,?,?);";
        insertStmt = new BatchedStatement(sql);
        
        sql = "update gros.comment set message=?, author=?, date=?, updater=?, updated_date=? where comment_id=?;";
        updateStmt = new BatchedStatement(sql);
    }
    
    private void getCheckStmt() throws SQLException, PropertyVetoException {
        if (checkStmt == null) {
            Connection con = insertStmt.getConnection();
            checkStmt = con.prepareStatement("select message, author, date, updater, updated_date from gros.comment where comment_id=?;");
        }
    }
    
    /**
     * Check whether a comment exists in the database and has the same properties.
     * @param comment_id The internal identifier of the comment
     * @param message The current contents of the message
     * @param author The shorthand name of the original author of the comment
     * @param date The timestamp when the comment was originally written
     * @param updater The shorthand name of the most recent editor of the comment
     * @param updated_date The timestamp when the comment was most recently edited
     * or the creation date if it has not been edited
     * @return An indicator of the state of the database regarding the given comment.
     * This is CheckResult.MISSING if the comment with the provided identifier
     * does not exist. This is CheckResult.DIFFERS if there is a row with the
     * provided identifier in the database, but it has different values
     * in its fields (for example without the most recent edit). This is
     * CheckResult.EXISTS if there is a comment in the database that matches all 
     * the provided parameters.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public CheckResult check_comment(int comment_id, String message, String author, Timestamp date, String updater, Timestamp updated_date) throws SQLException, PropertyVetoException {
        getCheckStmt();
        
        checkStmt.setInt(1, comment_id);
        CheckResult result;
        try (ResultSet rs = checkStmt.executeQuery()) {
            result = CheckResult.MISSING;
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
        }
        
        return result;
    }
    
    /**
     * Insert a new comment in the database.
     * @param comment_id The internal identifier of the comment
     * @param issue_id The internal identifier of the issue that the comment
     * belongs to
     * @param message The current contents of the message
     * @param author The shorthand name of the original author of the comment
     * @param date The timestamp when the comment was originally written
     * @param updater The shorthand name of the most recent editor of the comment
     * @param updated_date The timestamp when the comment was most recently edited
     * or the creation date if it has not been edited
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_comment(int comment_id, int issue_id, String message, String author, Timestamp date, String updater, Timestamp updated_date) throws SQLException, PropertyVetoException {    
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
    
    /**
     * Update an existing comment in the database with edited values.
     * @param comment_id The internal identifier of the comment
     * @param message The current contents of the message
     * @param author The shorthand name of the original author of the comment
     * @param date The timestamp when the comment was originally written
     * @param updater The shorthand name of the most recent editor of the comment
     * @param updated_date The timestamp when the comment was most recently edited
     * or the creation date if it has not been edited
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void update_comment(int comment_id, String message, String author, Timestamp date, String updater, Timestamp updated_date) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = updateStmt.getPreparedStatement();
        pstmt.setString(1, message);
        pstmt.setString(2, author);
        pstmt.setTimestamp(3, date);
        pstmt.setString(4, updater);
        pstmt.setTimestamp(5, updated_date);
        
        pstmt.setInt(6, comment_id);
        
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
    
