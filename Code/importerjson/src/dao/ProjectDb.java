/*
 * Copyright 2017-2018 ICTU
 * 
 * Importer of gathered software development metrics for MonetDB
 */
package dao;

import java.beans.PropertyVetoException;
import util.BaseDb;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Database access management for the projects table.
 * @author Enrique
 */
public class ProjectDb extends BaseDb implements AutoCloseable {
    private BatchedStatement insertStmt = null;
    private PreparedStatement checkIdStmt = null;
    private PreparedStatement checkNameStmt = null;
    private BatchedStatement updateStmt = null;
    
    public ProjectDb() {
        String sql = "insert into gros.project(name, main_project, github_team, gitlab_group, quality_name, quality_display_name, is_support_team) values (?,?,?,?,?,?,?);";
        insertStmt = new BatchedStatement(sql);
        sql = "update gros.project set main_project=?, github_team=?, gitlab_group=?, quality_name=?, quality_display_name=?, is_support_team=? where project_id=?;";
        updateStmt = new BatchedStatement(sql);
    }
    
    private void getCheckIdStatement() throws SQLException, PropertyVetoException {
        if (checkIdStmt == null) {
            Connection con = insertStmt.getConnection();
            String sql = "SELECT UPPER(name) AS name FROM gros.project WHERE project_id = ?";
            checkIdStmt = con.prepareStatement(sql);
        }
    }
    
    private void getCheckNameStatement() throws SQLException, PropertyVetoException {
        if (checkNameStmt == null) {
            Connection con = insertStmt.getConnection();
            String sql = "SELECT project_id FROM gros.project WHERE UPPER(name) = ?";
            checkNameStmt = con.prepareStatement(sql);
        }
    }
    
    /**
     * Insert a new project in the database.
     * @param name The shorthand name of the project
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_project(String name) throws SQLException, PropertyVetoException {
        insert_project(name, null, null, null, null, null, null);
    }

    /**
     * Insert a new project including metadata in the database.
     * @param name The shorthand name of the project
     * @param main_project The JIRA shorthand name of the main project, as used
     * in issue keys, or null if not known/set.
     * @param github_team The team name of the GitHub team working on the project,
     * or null if not known/set.
     * @param gitlab_group The group name of the GitLab group holding repositories
     * for the project, or null if not known/set.
     * @param quality_name The name of the project in the quality dashboard
     * project definitions, or null if not known/set.
     * @param quality_display_name The name of the project as shown in the quality
     * dashboard interface, or null if not known/set.
     * @param is_support_team Whether the project is maintained by a support team,
     * or null if not known/set.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */    
    public void insert_project(String name, String main_project, String github_team, String gitlab_group, String quality_name, String quality_display_name, Boolean is_support_team) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertStmt.getPreparedStatement();
        
        pstmt.setString(1, name);
        setString(pstmt, 2, main_project);
        setString(pstmt, 3, github_team);
        setString(pstmt, 4, gitlab_group);
        setString(pstmt, 5, quality_name);
        setString(pstmt, 6, quality_display_name);
        setBoolean(pstmt, 7, is_support_team);
        
        // Execute immediately because we need to have the row available.
        pstmt.execute();
    }

    /**
     * Update an existing project with metadata in the database.
     * @param project_id The internal project ID in the table
     * @param main_project The JIRA shorthand name of the main project, as used
     * in issue keys, or null if not known/set.
     * @param github_team The team name of the GitHub team working on the project,
     * or null if not known/set.
     * @param gitlab_group The group name of the GitLab group holding repositories
     * for the project, or null if not known/set.
     * @param quality_name The name of the project in the quality dashboard
     * project definitions, or null if not known/set.
     * @param quality_display_name The name of the project as shown in the quality
     * dashboard interface, or null if not known/set.
     * @param is_support_team Whether the project is maintained by a support team,
     * or null if not known/set.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */    
    public void update_project(int project_id, String main_project, String github_team, String gitlab_group, String quality_name, String quality_display_name, Boolean is_support_team) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = updateStmt.getPreparedStatement();
        
        setString(pstmt, 1, main_project);
        setString(pstmt, 2, github_team);
        setString(pstmt, 3, gitlab_group);
        setString(pstmt, 4, quality_name);
        setString(pstmt, 5, quality_display_name);
        setBoolean(pstmt, 6, is_support_team);
        
        pstmt.setInt(7, project_id);
        
        updateStmt.batch();
    }
    
    /**
     * Check whether the project with the given ID exists in the database
     * @param id The identifier of the project
     * @return The uppercase name of the project if it exists in the database,
     * or null if the project is not yet in the database.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public String check_project(int id) throws SQLException, PropertyVetoException {
        getCheckIdStatement();
        String name = null;
        
        checkIdStmt.setInt(1, id);
        
        try (ResultSet rs = checkIdStmt.executeQuery()) {
            if (rs.next()) {
                name = rs.getString("name");
            }
        }
        
        return name;
    }

    /**
     * Check whether the project with the given name exists in the database
     * @param name The shorthand name of the project
     * @return The identifier of the project if it exists in the database, or 0
     * if the project is not yet in the database.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public int check_project(String name) throws SQLException, PropertyVetoException {
        getCheckNameStatement();
        int idProject = 0;
        
        checkNameStmt.setString(1, name.toUpperCase().trim());
        
        try (ResultSet rs = checkNameStmt.executeQuery()) {
            if (rs.next()) {
                idProject = rs.getInt("project_id");
            }
        }
        
        return idProject;
    }

    @Override
    public void close() throws SQLException {
        // All inserts have already been executed
        insertStmt.close();
        
        updateStmt.execute();
        updateStmt.close();
        
        if (checkIdStmt != null) {
            checkIdStmt.close();
            checkIdStmt = null;
        }
        
        if (checkNameStmt != null) {
            checkNameStmt.close();
            checkNameStmt = null;
        }
    }

}
    
