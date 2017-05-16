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
 *
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
