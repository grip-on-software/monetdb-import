/**
 * Reservation table.
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
import java.sql.Timestamp;
import util.BaseDb;

/**
 * Database access management for reservations.
 * @author Leon Helwerda
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
    
    /**
     * Check whether a reservation with the provided identifier exists in the database.
     * @param reservation_id The reservation key
     * @return Whether there is a reservation with the given key
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
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
    
    /**
     * Insert a new reservation into the database
     * @param reservation_id The reservation key
     * @param project_id The project this reservation belongs to
     * @param requester The name of the person who planned the reservation
     * @param number_of_people The number of people that the reservation encompasses
     * @param description The text accompanying the reservation
     * @param start_date The timestamp at which the reservation starts
     * @param end_date The timestamp at which the reservation ends
     * @param prepare_date The timestamp at which the reservation is booked to
     * perform setup in the room. If no preparation is needed, then this is the
     * same as the start date.
     * @param close_date The timestamp until which the reservation is booked to
     * break down any setup in the room. If no dismantling is needed, then this
     * is the same as the end date.
     * @param sprint_id The sprint in which the reservation takes place
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
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
