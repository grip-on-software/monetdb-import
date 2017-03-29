/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import java.beans.PropertyVetoException;
import java.io.IOException;
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
public class UpdateDb extends BaseDb implements AutoCloseable {
    BatchedStatement insertStmt = null;
    PreparedStatement checkStmt = null;
    BatchedStatement updateStmt = null;
    
    public UpdateDb() {
        insertStmt = new BatchedStatement("insert into gros.update_tracker(project_id,filename,contents,update_date) values (?,?,?,?);");
        updateStmt = new BatchedStatement("update gros.update_tracker set contents=?, update_date=? where project_id=? and filename=?");
    }
    
    private void getCheckStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkStmt == null) {
            Connection con = insertStmt.getConnection();
            checkStmt = con.prepareStatement("select project_id, filename from gros.update_tracker where project_id=? and filename=?");
        }
    }
    
    public boolean check_file(int project_id, String filename) throws SQLException, IOException, PropertyVetoException {
        getCheckStmt();
        
        checkStmt.setInt(1, project_id);
        checkStmt.setString(2, filename);
        try (ResultSet rs = checkStmt.executeQuery()) {
            if (rs.next()) {
                return true;
            }
        }
        
        return false;
    }
    
    public void insert_file(int project_id, String filename, String contents, Timestamp update_date) throws SQLException, IOException, PropertyVetoException {
        PreparedStatement pstmt = insertStmt.getPreparedStatement();
        
        pstmt.setInt(1, project_id);
        pstmt.setString(2, filename);
        pstmt.setString(3, contents);
        pstmt.setTimestamp(4, update_date);
        
        insertStmt.batch();
    }
    
    public void update_file(int project_id, String filename, String contents, Timestamp update_date) throws SQLException, IOException, PropertyVetoException {
        PreparedStatement pstmt = updateStmt.getPreparedStatement();
        
        pstmt.setString(1, contents);
        pstmt.setTimestamp(2, update_date);

        pstmt.setInt(3, project_id);
        pstmt.setString(4, filename);
        
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
