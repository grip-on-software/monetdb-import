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
 * Database access management for the VCS events table.
 * @author Leon Helwerda
 */
public class VcsEventDb extends BaseDb implements AutoCloseable {
    private BatchedStatement insertStmt = null;
    private PreparedStatement checkStmt = null;
        
    public VcsEventDb() {
        String sql = "insert into gros.vcs_event(repo_id, action, kind, version_id, ref, date, developer_id) values (?,?,?,?,?,?,?)";
        insertStmt = new BatchedStatement(sql);
    }
    
    private void getCheckStmt() throws SQLException, PropertyVetoException {
        if (checkStmt == null) {
            Connection con = insertStmt.getConnection();
            checkStmt = con.prepareStatement("select 1 from gros.vcs_event where repo_id = ? and action = ? and kind = ? and version_id = ? and ref = ? and date = ? and developer_id = ?");
        }
    }
    
    /**
     * Insert the given event from the version control repository.
     * @param repo_id The identifier of the repository
     * @param action The action that the event performs, such as 'pushed to',
     * 'pushed new' or 'deleted'
     * @param kind The type of event, which provides details of what the event adds,
     * in addition to the action. Examples: 'push', 'tag_push'
     * @param commit_id The version to which the event applies
     * @param ref The Git reference that the event applies to
     * @param date The timestamp at which the event occurred
     * @param developer_id The identifier of the VCS developer that performed
     * the action in the version control repository
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_event(int repo_id, String action, String kind, String commit_id, String ref, Timestamp date, int developer_id) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertStmt.getPreparedStatement();
        pstmt.setInt(1, repo_id);
        pstmt.setString(2, action);
        pstmt.setString(3, kind);
        pstmt.setString(4, commit_id);
        pstmt.setString(5, ref);
        pstmt.setTimestamp(6, date);
        pstmt.setInt(7, developer_id);
        
        insertStmt.batch();
    }

    /**
     * Check whether the given event from the version control repository exists
     * in the database.
     * @param repo_id The identifier of the repository
     * @param action The action that the event performs, such as 'pushed to',
     * 'pushed new' or 'deleted'
     * @param kind The type of event, which provides details of what the event adds,
     * in addition to the action. Examples: 'push', 'tag_push'
     * @param commit_id The version to which the event applies
     * @param ref The Git reference that the event applies to
     * @param date The timestamp at which the event occurred
     * @param developer_id The identifier of the VCS developer that performed
     * the action in the version control repository
     * @return Whether the event exists in the database
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public boolean check_event(int repo_id, String action, String kind, String commit_id, String ref, Timestamp date, int developer_id) throws SQLException, PropertyVetoException {
        getCheckStmt();
        
        checkStmt.setInt(1, repo_id);
        checkStmt.setString(2, action);
        checkStmt.setString(3, kind);
        checkStmt.setString(4, commit_id);
        checkStmt.setString(5, ref);
        checkStmt.setTimestamp(6, date);
        checkStmt.setInt(7, developer_id);
        
        try (ResultSet rs = checkStmt.executeQuery()) {
            if (rs.next()) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void close() throws SQLException {
        insertStmt.execute();
        insertStmt.close();
        
        if (checkStmt != null) {
            checkStmt.close();
            checkStmt = null;
        }
    }
    
}
