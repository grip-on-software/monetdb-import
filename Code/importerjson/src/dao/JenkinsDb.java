/**
 * Jenkins status table.
 * 
 * Copyright 2017-2020 ICTU
 * Copyright 2017-2022 Leiden University
 * Copyright 2017-2023 Leon Helwerda
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
import util.BaseDb;

/**
 * Database access management for Jenkins instances.
 * @author Leon Helwerda
 */
public class JenkinsDb extends BaseDb implements AutoCloseable {
    private BatchedStatement insertStmt = null;
    private PreparedStatement checkStmt = null;
    private BatchedStatement updateStmt = null;

    public enum CheckResult {
        MISSING, DIFFERS, EXISTS
    }

    public JenkinsDb() {
        String sql = "insert into gros.jenkins(project_id,host,jobs,views,nodes) values (?,?,?,?,?);";
        insertStmt = new BatchedStatement(sql);
        
        sql = "update gros.jenkins set jobs=?, views=?, nodes=? where project_id=? and host=?;";
        updateStmt = new BatchedStatement(sql);
    }
    
    private void getCheckStmt() throws SQLException, PropertyVetoException {
        if (checkStmt == null) {
            Connection con = insertStmt.getConnection();
            checkStmt = con.prepareStatement("select jobs, views, nodes from gros.jenkins where project_id=? and host=?;");
        }
    }
    
    /**
     * Check whether the Jenkins instance for this project has usage statistics
     * registered in the database, and whether it has the same values.
     * @param project_id Internal identifier of the project
     * @param host Base host URL of the Jenkins instance
     * @param jobs Number of jobs on the Jenkins instance
     * @param views Number of views on the Jenkins instance
     * @param nodes Number of computer nodes attached to the Jenkins instance
     * @return An indicator of the state of the database regarding the Jenkins
     * instance. This is CheckResult.MISSING if the instance for the provided host
     * does not exist. This is CheckResult.DIFFERS if there is a row for the
     * provided host in the database, but it has different values in its fields.
     * This is CheckResult.EXISTS if there is a Jenkins instance in the database 
     * that matches all the provided parameters.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public CheckResult check(int project_id, String host, int jobs, int views, int nodes) throws SQLException, PropertyVetoException {
        getCheckStmt();
        
        checkStmt.setInt(1, project_id);
        checkStmt.setString(2, host);
        
        try (ResultSet rs = checkStmt.executeQuery()) {
            if (!rs.next()) {
                return CheckResult.MISSING;
            }
            if (rs.getInt("jobs") == jobs && rs.getInt("views") == views && rs.getInt("nodes") == nodes) {
                return CheckResult.EXISTS;
            }
            return CheckResult.DIFFERS;
        }
    }
    
    /**
     * Insert a new row for a Jenkins instance into the database.
     * @param project_id Internal identifier of the project
     * @param host Base host URL of the Jenkins instance
     * @param jobs Number of jobs on the Jenkins instance
     * @param views Number of views on the Jenkins instance
     * @param nodes Number of computer nodes attached to the Jenkins instance
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert(int project_id, String host, int jobs, int views, int nodes) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertStmt.getPreparedStatement();
        
        pstmt.setInt(1, project_id);
        pstmt.setString(2, host);
        pstmt.setInt(3, jobs);
        pstmt.setInt(4, views);
        pstmt.setInt(5, nodes);
        
        insertStmt.batch();
    }
    
    /**
     * Update an existing row for a Jenkins instance in the database with new
     * usage statistics fields.
     * @param project_id Internal identifier of the project
     * @param host Base host URL of the Jenkins instance
     * @param jobs Number of jobs on the Jenkins instance
     * @param views Number of views on the Jenkins instance
     * @param nodes Number of computer nodes attached to the Jenkins instance
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void update(int project_id, String host, int jobs, int views, int nodes) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = updateStmt.getPreparedStatement();
        
        pstmt.setInt(1, jobs);
        pstmt.setInt(2, views);
        pstmt.setInt(3, nodes);

        pstmt.setInt(4, project_id);
        pstmt.setString(5, host);
        
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
