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
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Objects;
import util.BaseLinkDb;

/**
 * Database access management for the JIRA components and issue components tables.
 * @author Leon Helwerda
 */
public class ComponentDb extends BaseLinkDb implements AutoCloseable {
    /**
     * Object containing the relationship properties of an issue link.
     */
    private static class Link {
        /**
         * Identifier of the JIRA issue involved in the link.
         */
        public final int issue_id;
        /**
         * Identifier of the JIRA component involved in the link.
         */
        public final int component_id;
        
        public Link(int issue_id, int component_id) {
            this.issue_id = issue_id;
            this.component_id = component_id;
        }
        
        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (other instanceof Link) {
                Link otherLink = (Link) other;
                return (issue_id == otherLink.issue_id &&
                        component_id == otherLink.component_id);
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            int hash = Objects.hashCode(this.issue_id);
            hash = 13 * hash + Objects.hashCode(this.component_id);
            return hash;
        }
    }
    
    private HashMap<Link, LinkDates> linkCache = null;
    
    private BatchedStatement insertLinkStmt = null;
    private PreparedStatement checkLinkStmt = null;
    private BatchedStatement updateLinkStmt = null;
    
    private BatchedStatement insertComponentStmt = null;
    private PreparedStatement checkComponentStmt = null;
    private BatchedStatement updateComponentStmt = null;
    
    private final int NAME_LENGTH = 100;
    private final int DESCRIPTION_LENGTH = 500;

    public ComponentDb() {
        String sql = "insert into gros.issue_component (issue_id,component_id,start_date,end_date) values (?,?,?,?);";
        insertLinkStmt = new BatchedStatement(sql);

        sql = "update gros.issue_component set start_date=?, end_date=? where issue_id=? and component_id=?;";
        updateLinkStmt = new BatchedStatement(sql);
        
        sql = "insert into gros.component (project_id,component_id,name,description) values (?,?,?,?);";
        insertComponentStmt = new BatchedStatement(sql);
        
        sql = "update gros.component set name=?, description=? where project_id=? and component_id=?;";
        updateComponentStmt = new BatchedStatement(sql);
    }

    private void getCheckLinkStmt() throws SQLException, PropertyVetoException {
        if (checkLinkStmt == null) {
            Connection con = insertLinkStmt.getConnection();
            checkLinkStmt = con.prepareStatement("select start_date, end_date from gros.issue_component where issue_id=? and component_id=?;");
        }
    }

    private void fillLinkCache() throws SQLException, PropertyVetoException {
        if (linkCache != null) {
            return;
        }
        linkCache = new HashMap<>();
        
        Connection con = insertLinkStmt.getConnection();
        String sql = "SELECT issue_id, component_id, start_date, end_date FROM gros.issue_component";
        try (
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql)
        ) {
            while (rs.next()) {
                Link link = new Link(rs.getInt("issue_id"), rs.getInt("component_id"));
                LinkDates dates = new LinkDates(rs.getTimestamp("start_date"), rs.getTimestamp("end_date"));
                linkCache.put(link, dates);
            }
        }
    }

    /**
     * Check whether a link between an issue and a component exists and it has
     * the same properties as the provided arguments.
     * @param issue_id Identifier of the issue which has a component.
     * @param component_id Identifier of the component that the issue is in.
     * @param start_date Timestamp since which the link exists, or null if the start date is unknown
     * @param end_date Timestamp at which the link ceases to exist, or null if the link still exists
     * @return An indicator of the state of the database regarding the given component link.
     * This CheckResult has state MISSING if the component link between the provided issue_id and
     * component_id does not exist. The state is DIFFERS if there is a row with the
     * provided issue_id and component_id in the database, but it has different values
     * in its fields. The state is EXISTS if there is a component link in
     * the database that matches all the provided parameters.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public CheckResult check_link(int issue_id, int component_id, Timestamp start_date, Timestamp end_date) throws SQLException, PropertyVetoException {
        getCheckLinkStmt();
        fillLinkCache();
        
        Link link = new Link(issue_id, component_id);
        LinkDates dates = new LinkDates(start_date, end_date);
                
        LinkDates cache_dates = linkCache.get(link);
        if (cache_dates != null) {
            return compareLinkDates(dates, cache_dates);
        }
        
        CheckResult result = new CheckResult();
        
        checkLinkStmt.setInt(1, issue_id);
        checkLinkStmt.setInt(2, component_id);
        try (ResultSet rs = checkLinkStmt.executeQuery()) {
            while (rs.next()) {
                LinkDates row_dates = new LinkDates(rs.getTimestamp("start_date"), rs.getTimestamp("end_date"));
                result = compareLinkDates(dates, row_dates);
                linkCache.put(link, row_dates);
            }
        }
        
        return result;
    }

    /**
     * Insert a new component link in the database.
     * @param issue_id Identifier of the issue which has a component.
     * @param component_id Identifier of the component that the issue is in.
     * @param start_date Timestamp since which the link exists, or null if the start date is unknown
     * @param end_date Timestamp at which the link ceases to exist, or null if the link still exists
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_link(int issue_id, int component_id, Timestamp start_date, Timestamp end_date) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertLinkStmt.getPreparedStatement();
        pstmt.setInt(1, issue_id);
        pstmt.setInt(2, component_id);
        setTimestamp(pstmt, 3, start_date);
        setTimestamp(pstmt, 4, end_date);
                             
        insertLinkStmt.batch();
        
        if (linkCache != null) {
            Link link = new Link(issue_id, component_id);
            LinkDates dates = new LinkDates(start_date, end_date);
            linkCache.put(link, dates);
        }
    }
    
    /**
     * Update an existing component link in the database with new start and end dates.
     * @param issue_id Identifier of the issue which has a component.
     * @param component_id Identifier of the component that the issue is in.
     * @param start_date Timestamp since which the link exists, or null if the start date is unknown
     * @param end_date Timestamp at which the link ceases to exist, or null if the link still exists
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void update_link(int issue_id, int component_id, Timestamp start_date, Timestamp end_date) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = updateLinkStmt.getPreparedStatement();
        setTimestamp(pstmt, 1, start_date);
        setTimestamp(pstmt, 2, end_date);
        
        pstmt.setInt(3, issue_id);
        pstmt.setInt(4, component_id);

        updateLinkStmt.batch();
        
        if (linkCache != null) {
            Link link = new Link(issue_id, component_id);
            LinkDates dates = new LinkDates(start_date, end_date);
            linkCache.put(link, dates);
        }
    }

    private void getCheckComponentStmt() throws SQLException, PropertyVetoException {
        if (checkComponentStmt == null) {
            Connection con = insertComponentStmt.getConnection();
            checkComponentStmt = con.prepareStatement("select name, description from gros.component where project_id=? and component_id=?;");
        }
    }
    
    /**
     * Check whether a component exists in the database and it has the given properties.
     * @param project_id Identifier of the project in which the component exists.
     * @param component_id Identifier of the component.
     * @param name Short name of the component.
     * @param description Longer description of the component, or null if it is
     * not provided.
     * @return An indicator of the state of the database regarding the given component.
     * This is CheckResult.State.MISSING if the component with the given project ID
     * and component ID does not exist. This is CheckResult.State.DIFFERS if the given
     * project ID and component ID exist in the database, but the name and description
     * are different. This is CheckResult.State.EXISTS if there is a component in
     * the database and all properties match with the row.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public CheckResult.State check_component(int project_id, int component_id, String name, String description) throws SQLException, PropertyVetoException {
        getCheckComponentStmt();
        
        checkComponentStmt.setInt(1, project_id);
        checkComponentStmt.setInt(2, component_id);
        
        CheckResult.State state = CheckResult.State.MISSING;
        try (ResultSet rs = checkComponentStmt.executeQuery()) {
            if (rs.next()) {
                if (name.equals(rs.getString("name")) &&
                    description == null ? rs.getObject("description") == null : description.equals(rs.getString("description"))) {
                    state = CheckResult.State.EXISTS;
                }
                else {
                    state = CheckResult.State.DIFFERS;
                }
            }
        }
        
        return state;
    }
    
    /**
     * Insert a new component in the database.
     * @param project_id Identifier of the project in which the component exists.
     * @param component_id Identifier of the component.
     * @param name Short name of the component.
     * @param description Longer description of the component, or null if it is
     * not provided.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_component(int project_id, int component_id, String name, String description) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertComponentStmt.getPreparedStatement();
        
        pstmt.setInt(1, project_id);
        pstmt.setInt(2, component_id);
        setString(pstmt, 3, name, NAME_LENGTH);
        setString(pstmt, 4, description, DESCRIPTION_LENGTH);
        
        insertComponentStmt.batch();
    }
    
    /**
     * Update an existing component in the database.
     * @param project_id Identifier of the project in which the component exists.
     * @param component_id Identifier of the component.
     * @param name Short name of the component.
     * @param description Longer description of the component, or null if it is
     * not provided.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void update_component(int project_id, int component_id, String name, String description) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = updateComponentStmt.getPreparedStatement();
        
        setString(pstmt, 1, name, NAME_LENGTH);
        setString(pstmt, 2, description, DESCRIPTION_LENGTH);
        
        pstmt.setInt(3, project_id);
        pstmt.setInt(4, component_id);
        
        updateComponentStmt.batch();
    }

    @Override
    public void close() throws SQLException {
        insertLinkStmt.execute();
        insertLinkStmt.close();
        
        updateLinkStmt.execute();
        updateLinkStmt.close();
        
        if (checkLinkStmt != null) {
            checkLinkStmt.close();
            checkLinkStmt = null;
        }
        
        if (linkCache != null) {
            linkCache.clear();
            linkCache = null;
        }
        
        insertComponentStmt.execute();
        insertComponentStmt.close();
        
        updateComponentStmt.execute();
        updateComponentStmt.close();
        
        if (checkComponentStmt != null) {
            checkComponentStmt.close();
            checkComponentStmt = null;
        }
    }
}
