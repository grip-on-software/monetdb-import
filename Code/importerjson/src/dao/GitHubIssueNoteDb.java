/**
 * GitHub issue note table.
 * 
 * Copyright 2017-2020 ICTU
 * Copyright 2017-2022 Leiden University
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
import java.sql.Timestamp;
import util.BaseDb;

/**
 * Database access management for the GitHub issue note table.
 * @author Leon Helwerda
 */
public class GitHubIssueNoteDb extends BaseDb implements AutoCloseable {
    private BatchedStatement insertNoteStmt = null;
    private PreparedStatement checkNoteStmt = null;
    private BatchedStatement updateNoteStmt = null;
    
    public enum CheckResult {
        MISSING, DIFFERS, EXISTS
    }
    
    public GitHubIssueNoteDb() {
        String sql = "insert into gros.github_issue_note(repo_id,issue_id,note_id,author_id,comment,created_date,updated_date) values (?,?,?,?,?,?,?);";
        insertNoteStmt = new BatchedStatement(sql);
        
        sql = "update gros.github_issue_note set author_id=?, comment=?, created_date=?, updated_date=? where repo_id=? AND issue_id=? AND note_id=?;";
        updateNoteStmt = new BatchedStatement(sql);
    }

    private void getCheckNoteStmt() throws SQLException, PropertyVetoException {
        if (checkNoteStmt == null) {
            Connection con = insertNoteStmt.getConnection();
            checkNoteStmt = con.prepareStatement("select author_id, comment, created_date, updated_date from gros.github_issue_note where repo_id=? AND issue_id=? AND note_id=?;");
        }
    }
    
    /**
     * Check whether a GitHub issue note exists in the database and that it has the
     * same properties as the provided parameters.
     * @param repo_id Idenitifer of the repository the issue is made for
     * @param issue_id Internal identifier for the issue
     * @param note_id Internal identifier for the note
     * @param author_id Identifier of the VCS developer that created the note
     * @param comment The text in the note
     * @param created_date Timestamp at which the note is created
     * @param updated_date Timestamp at which the note received an update
     * @return An indicator of the state of the database regarding the given note.
     * This is CheckResult.MISSING if the issue with the provided repository, issue
     * and note identifiers does not exist. This is CheckResult.DIFFERS if there
     * is a row with the provided repsoitory, issue and note identifiers in the database,
     * but it has different values in its fields. This is CheckResult.EXISTS if there
     * is an issue note in the database that matches all the provided parameters.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public CheckResult check_note(int repo_id, int issue_id, int note_id, int author_id, String comment, Timestamp created_date, Timestamp updated_date) throws SQLException, PropertyVetoException {
        getCheckNoteStmt();
        
        checkNoteStmt.setInt(1, repo_id);
        checkNoteStmt.setInt(2, issue_id);
        checkNoteStmt.setInt(3, note_id);
        CheckResult result;
        try (ResultSet rs = checkNoteStmt.executeQuery()) {
            result = CheckResult.MISSING;
            while (rs.next()) {
                if (author_id == rs.getInt("author_id") &&
                        comment.equals(rs.getString("comment")) &&
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
     * Insert a new GitHub issue note in the database.
     * @param repo_id Idenitifer of the repository the issue is made for
     * @param issue_id Internal identifier for the issue
     * @param note_id Internal identifier for the note
     * @param author_id Identifier of the VCS developer that created the note
     * @param comment The text in the note
     * @param created_date Timestamp at which the note is created
     * @param updated_date Timestamp at which the note received an update
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_note(int repo_id, int issue_id, int note_id, int author_id, String comment, Timestamp created_date, Timestamp updated_date) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertNoteStmt.getPreparedStatement();
        pstmt.setInt(1, repo_id);
        pstmt.setInt(2, issue_id);
        pstmt.setInt(3, note_id);
        pstmt.setInt(4, author_id);
        pstmt.setString(5, comment);
        pstmt.setTimestamp(6, created_date);
        pstmt.setTimestamp(7, updated_date);
                             
        insertNoteStmt.batch();
    }
    
    /**
     * Update an existing GitHub issue note in the database with new values.
     * @param repo_id Idenitifer of the repository the issue is made for
     * @param issue_id Internal identifier for the issue
     * @param note_id Internal identifier for the note
     * @param author_id Identifier of the VCS developer that created the note
     * @param comment The text in the note
     * @param created_date Timestamp at which the note is created
     * @param updated_date Timestamp at which the note received an update
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void update_note(int repo_id, int issue_id, int note_id, int author_id, String comment, Timestamp created_date, Timestamp updated_date) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = updateNoteStmt.getPreparedStatement();
        pstmt.setInt(1, author_id);
        pstmt.setString(2, comment);
        pstmt.setTimestamp(3, created_date);
        pstmt.setTimestamp(4, updated_date);
        
        pstmt.setInt(5, repo_id);
        pstmt.setInt(6, issue_id);
        pstmt.setInt(7, note_id);
        
        updateNoteStmt.batch();
    }
    
    @Override
    public void close() throws SQLException {
        insertNoteStmt.execute();
        insertNoteStmt.close();
        
        updateNoteStmt.execute();
        updateNoteStmt.close();
        
        if (checkNoteStmt != null) {
            checkNoteStmt.close();
            checkNoteStmt = null;
        }
    }

}
