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
 * @author Leon Helwera
 */
public class ReservationDb extends BaseDb implements AutoCloseable {
    private BatchedStatement insertStmt = null;
    private PreparedStatement checkStmt = null;

    public ReservationDb() {
        String sql = "insert into gros.reservation(reservation_id,project_id,requester,number_of_people,description,start_date,end_date,prepare_date,close_date,sprint_id) values (?,?,?,?,?,?,?,?,?,?);";
        insertStmt = new BatchedStatement(sql);        
    }
    
    private void getCheckStmt() throws SQLException, PropertyVetoException {
        if (checkStmt == null) {
            Connection con = insertStmt.getConnection();
            checkStmt = con.prepareStatement("select reservation_id from gros.reservation where reservation_id=?;");
        }
    }
        
    public boolean check_reservation(String reservation_id) throws SQLException, PropertyVetoException {
        getCheckStmt();
        
        checkStmt.setString(1, reservation_id);
        try (ResultSet rs = checkStmt.executeQuery()) {
            if (rs.next()) {
                return true;
            }
        }
        
        return false;
    }
    
    public void insert_reservation(String reservation_id, int project_id, String requester, int number_of_people, String description, Timestamp start_date, Timestamp end_date, Timestamp prepare_date, Timestamp close_date, int sprint_id) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertStmt.getPreparedStatement();
        pstmt.setString(1, reservation_id);
        pstmt.setInt(2, project_id);
        pstmt.setString(3, requester);
        pstmt.setInt(4, number_of_people);
        pstmt.setString(5, description);
        pstmt.setTimestamp(6, start_date);
        pstmt.setTimestamp(7, end_date);
        pstmt.setTimestamp(8, prepare_date);
        pstmt.setTimestamp(9, close_date);
        pstmt.setInt(10, sprint_id);
        
        insertStmt.batch();
    }

    @Override
    public void close() throws Exception {
        insertStmt.execute();
        insertStmt.close();
                
        if (checkStmt != null) {
            checkStmt.close();
            checkStmt = null;
        }
    }
}
