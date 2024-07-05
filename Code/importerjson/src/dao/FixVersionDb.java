/**
 * JIRA fix version table.
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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import util.BaseDb;

/**
 * Database access management for the JIRA fix versions.
 * @author Leon Helwerda
 */
public class FixVersionDb extends BaseDb implements AutoCloseable {
    private BatchedStatement insertStmt = null;
    private PreparedStatement checkStmt = null;
    private BatchedStatement updateStmt = null;

    public enum CheckResult {
        MISSING, DIFFERS, EXISTS
    }

    public FixVersionDb() {
        String sql = "insert into gros.fixversion(id,project_id,name,description,start_date,release_date,released) values (?,?,?,?,?,?,?);";
        insertStmt = new BatchedStatement(sql);
        
        sql = "update gros.fixversion set name=?, description=?, start_date=?, release_date=?, released=? where id=? and project_id=?;";
        updateStmt = new BatchedStatement(sql);
    }
    
    private void getCheckStmt() throws SQLException, PropertyVetoException {
        if (checkStmt == null) {
            Connection con = insertStmt.getConnection();
            checkStmt = con.prepareStatement("select name, description, start_date, release_date, released from gros.fixversion where id=? and project_id=?;");
        }
    }
    
    private boolean compareDates(Date date, Date current_date) {
        if (date == null) {
            return (current_date == null);
        }
        return date.equals(current_date);
    }
    
    /**
     * Check whether a fix version exists in the database and that it has the same
     * properties as the provided arguments.
     * @param id The internal identifier of the fix version
     * @param project_id The identifier of the project that the fix version applies to
     * @param name The name (version numbering scheme) of the release version.
     * @param description The description provided to the release version 
     * @param start_date The day on which work started on this fix version, or null
     * if work has not yet started
     * @param release_date The day on which the version is released or is supposed
     * to be released, or null if there is no projected date yet
     * @param released Whether the fix version has been released already
     * @return An indicator of the state of the database regarding the given fix version.
     * This is CheckResult.MISSING if the fix version with the provided identifier
     * does not exist. This is CheckResult.DIFFERS if there is a row with the
     * provided identifier in the database, but it has different values
     * in its fields. This is CheckResult.EXISTS if there is a fix version in
     * the database that matches all the provided parameters.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public CheckResult check_version(int id, int project_id, String name, String description, Date start_date, Date release_date, boolean released) throws SQLException, PropertyVetoException {
        getCheckStmt();
        
        checkStmt.setInt(1, id);
        checkStmt.setInt(2, project_id);
        CheckResult result;
        try (ResultSet rs = checkStmt.executeQuery()) {
            result = CheckResult.MISSING;
            while (rs.next()) {
                if (name.equals(rs.getString("name")) &&
                        (description == null ? rs.getObject("description") == null : description.equals(rs.getString("description"))) &&
                        compareDates(start_date, rs.getDate("start_date")) &&
                        compareDates(release_date, rs.getDate("release_date")) &&
                        released == rs.getBoolean("released")) {
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
     * Insert a new fix version in the database.
     * @param id The internal identifier of the fix version
     * @param project_id The identifier of the project that the fix version applies to
     * @param name The name (version numbering scheme) of the release version.
     * @param description The description provided to the release version 
     * @param start_date The day on which work started on this fix version, or null
     * if work has not yet started
     * @param release_date The day on which the version is released or is supposed
     * to be released, or null if there is no projected date yet
     * @param released Whether the fix version has been released already
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_version(int id, int project_id, String name, String description, Date start_date, Date release_date, boolean released) throws SQLException, PropertyVetoException {    
        PreparedStatement pstmt = insertStmt.getPreparedStatement();
        pstmt.setInt(1, id);
        pstmt.setInt(2, project_id);
        pstmt.setString(3, name);
        setString(pstmt, 4, description);
        setDate(pstmt, 5, start_date);
        setDate(pstmt, 6, release_date);
        pstmt.setBoolean(7, released);
                             
        insertStmt.batch();
    }
    
    /**
     * Update an existing fix version with new values in its fields.
     * @param id The internal identifier of the fix version
     * @param project_id The identifier of the project that the fix version applies to
     * @param name The name (version numbering scheme) of the release version.
     * @param description The description provided to the release version 
     * @param start_date The day on which work started on this fix version, or null
     * if work has not yet started
     * @param release_date The day on which the version is released or is supposed
     * to be released, or null if there is no projected date yet
     * @param released Whether the fix version has been released already
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void update_version(int id, int project_id, String name, String description, Date start_date, Date release_date, boolean released) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = updateStmt.getPreparedStatement();
        pstmt.setString(1, name);
        setString(pstmt, 2, description);
        setDate(pstmt, 3, start_date);
        setDate(pstmt, 4, release_date);
        pstmt.setBoolean(5, released);
        
        pstmt.setInt(6, id);
        pstmt.setInt(7, project_id);
        
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
