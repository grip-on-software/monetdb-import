/**
 * Seat count table.
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
 * Database access management for seat counts.
 * @author Leon Helwerda
 */
public class SeatDb extends BaseDb implements AutoCloseable {
    private BatchedStatement insertStmt = null;
    private PreparedStatement checkStmt = null;
    private BatchedStatement updateStmt = null;

    public SeatDb() {
        String sql = "insert into gros.seats(project_id,sprint_id,date,seats) values (?,?,?,?);";
        insertStmt = new BatchedStatement(sql);
        
        sql = "update gros.seats set seats=? where project_id=? and sprint_id=? and date=?;";
        updateStmt = new BatchedStatement(sql);
    }
    
    private void getCheckStmt() throws SQLException, PropertyVetoException {
        if (checkStmt == null) {
            Connection con = insertStmt.getConnection();
            checkStmt = con.prepareStatement("select seats from gros.seats where project_id=? and sprint_id=? and date=?;");
        }
    }
    
    /**
     * Check whether the Jenkins instance for this project has usage statistics
     * registered in the database, and whether it has the same values.
     * @param project_id Internal identifier of the project
     * @param sprint_id Sprint identifier of the project
     * @param month The month to which the seat count applies
     * @return The seat count for the given sprint or null if the seat count for
     * the sprint is not known.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public Float check(int project_id, int sprint_id, Timestamp month) throws SQLException, PropertyVetoException {
        getCheckStmt();
        
        checkStmt.setInt(1, project_id);
        checkStmt.setInt(2, sprint_id);
        checkStmt.setTimestamp(3, month);
        
        try (ResultSet rs = checkStmt.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            
            return rs.getFloat("seats");
        }
    }
    
    /**
     * Insert a new row for a seat count into the database.
     * @param project_id Internal identifier of the project
     * @param sprint_id Sprint identifier of the project
     * @param month The month to which the seat count applies
     * @param seats The number of seats
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert(int project_id, int sprint_id, Timestamp month, float seats) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertStmt.getPreparedStatement();
        
        pstmt.setInt(1, project_id);
        pstmt.setInt(2, sprint_id);
        pstmt.setTimestamp(3, month);
        pstmt.setFloat(4, seats);
        
        insertStmt.batch();
    }
    
    /**
     * Update an existing row for a seat count in the database with a new count.
     * @param project_id Internal identifier of the project
     * @param sprint_id Sprint identifier of the project
     * @param month The month to which the seat count applies
     * @param seats The number of seats
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void update(int project_id, int sprint_id, Timestamp month, float seats) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = updateStmt.getPreparedStatement();
        
        pstmt.setFloat(1, seats);

        pstmt.setInt(2, project_id);
        pstmt.setInt(3, sprint_id);
        pstmt.setTimestamp(4, month);
        
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
