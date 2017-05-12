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
 * Database access magement for sprint properties.
 * @author Leon Helwerda
 */
public class SprintDb extends BaseDb implements AutoCloseable {
    private BatchedStatement insertStmt = null;
    private PreparedStatement checkStmt = null;
    private BatchedStatement updateStmt = null;
    private PreparedStatement findStmt = null;

    public enum CheckResult {
        MISSING, DIFFERS, EXISTS
    };

    public SprintDb() {
        String sql = "insert into gros.sprint(sprint_id,project_id,name,start_date,end_date,complete_date) values (?,?,?,?,?,?);";
        insertStmt = new BatchedStatement(sql);
        
        sql = "update gros.sprint set name=?, start_date=?, end_date=?, complete_date=? where sprint_id=? and project_id=?;";
        updateStmt = new BatchedStatement(sql);
    }
    
    private void getCheckStmt() throws SQLException, IOException, PropertyVetoException {
        if (checkStmt == null) {
            Connection con = insertStmt.getConnection();
            checkStmt = con.prepareStatement("select name, start_date, end_date, complete_date from gros.sprint where sprint_id=? and project_id=?;");
        }
    }
    
    private boolean compareTimestamps(Timestamp date, Timestamp current_date) {
        if (date == null) {
            return (current_date == null);
        }
        return date.equals(current_date);
    }
    
    /**
     * Check whether a certain sprint exists in the the database and has the same
     * properties as the provided parameters.
     * @param sprint_id The internal sprint ID from JIRA.
     * @param project_id The project identifier.
     * @param name The human-readable name of the sprint as provided in JIRA.
     * @param start_date The date at which the sprint starts or is set to start.
     * @param end_date The date at which the sprint ends or is set to end.
     * @param complete_date The date at which the tasks in the sprint are completed.
     * @return The check result: CheckResult.MISSING if the sprint and project ID
     * combination is not found, CheckResult.EXISTS if all properties match, or
     * CheckResult.DIFFERS if the sprint and project ID is found but the name and/or
     * dates do not match.
     * @throws SQLException
     * @throws IOException
     * @throws PropertyVetoException 
     */
    public CheckResult check_sprint(int sprint_id, int project_id, String name, Timestamp start_date, Timestamp end_date, Timestamp complete_date) throws SQLException, IOException, PropertyVetoException {
        getCheckStmt();
        
        checkStmt.setInt(1, sprint_id);
        checkStmt.setInt(2, project_id);
        CheckResult result;
        try (ResultSet rs = checkStmt.executeQuery()) {
            result = CheckResult.MISSING;
            while (rs.next()) {
                if (name.equals(rs.getString("name")) &&
                        compareTimestamps(start_date, rs.getTimestamp("start_date")) &&
                        compareTimestamps(end_date, rs.getTimestamp("end_date")) &&
                        compareTimestamps(complete_date, rs.getTimestamp("complete_date"))) {
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
     * Insert a new sprint in the database.
     * @param sprint_id The internal sprint ID from JIRA.
     * @param project_id The project identifier.
     * @param name The human-readable name of the sprint as provided in JIRA.
     * @param start_date The date at which the sprint starts or is set to start.
     * @param end_date The date at which the sprint ends or is set to end.
     * @param complete_date The date at which the tasks in the sprint are completed.
     * @throws SQLException
     * @throws IOException
     * @throws PropertyVetoException 
     */
    public void insert_sprint(int sprint_id, int project_id, String name, Timestamp start_date, Timestamp end_date, Timestamp complete_date) throws SQLException, IOException, PropertyVetoException {
        PreparedStatement pstmt = insertStmt.getPreparedStatement();
        pstmt.setInt(1, sprint_id);
        pstmt.setInt(2, project_id);
        pstmt.setString(3, name);
        setTimestamp(pstmt, 4, start_date);
        setTimestamp(pstmt, 5, end_date);
        setTimestamp(pstmt, 6, complete_date);

        insertStmt.batch();
    }
    
    /**
     * Update an existing sprint in the database to hold different properties.
     * @param sprint_id The internal sprint ID from JIRA.
     * @param project_id The project identifier.
     * @param name The human-readable name of the sprint as provided in JIRA.
     * @param start_date The date at which the sprint starts or is set to start.
     * @param end_date The date at which the sprint ends or is set to end.
     * @param complete_date The date at which the tasks in the sprint are completed.
     * @throws SQLException
     * @throws IOException
     * @throws PropertyVetoException 
     */
    public void update_sprint(int sprint_id, int project_id, String name, Timestamp start_date, Timestamp end_date, Timestamp complete_date) throws SQLException, IOException, PropertyVetoException {
        PreparedStatement pstmt = updateStmt.getPreparedStatement();
        pstmt.setString(1, name);
        setTimestamp(pstmt, 2, start_date);
        setTimestamp(pstmt, 3, end_date);
        setTimestamp(pstmt, 4, complete_date);
        
        pstmt.setInt(5, sprint_id);
        pstmt.setInt(6, project_id);
        
        updateStmt.batch();
    }
    
    private void getFindStmt() throws SQLException, IOException, PropertyVetoException {
        if (findStmt == null) {
            Connection con = insertStmt.getConnection();
            findStmt = con.prepareStatement("select sprint_id, start_date, end_date from gros.sprint where project_id=? and ? BETWEEN start_date AND end_date ORDER BY start_date;");
        }
    }
    
    /**
     * Find a sprint for the given project that contains the provided date.
     * @param project_id The project identifier to use sprints from.
     * @param date The date to contain in the sprint.
     * @return The sprint ID of the latest sprint that contains the date, or 0
     * if there are no sprints that contain the date.
     * @throws SQLException
     * @throws IOException
     * @throws PropertyVetoException 
     */
    public int find_sprint(int project_id, Timestamp date) throws SQLException, IOException, PropertyVetoException {
        getFindStmt();
        
        findStmt.setInt(1, project_id);
        findStmt.setTimestamp(2, date);
        int sprint_id = 0;
        try (ResultSet rs = findStmt.executeQuery()) {
            while(rs.next()) {
                sprint_id = rs.getInt("sprint_id");
            }
        }
        
        return sprint_id;
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

        if (findStmt != null) {
            findStmt.close();
            findStmt = null;
        }
    }
    
}
