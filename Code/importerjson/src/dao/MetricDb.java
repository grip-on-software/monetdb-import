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
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Database access management for the metrics tables.
 * @author Enrique, Leon Helwerda
 */
public class MetricDb extends BaseDb implements AutoCloseable {
    private PreparedStatement checkMetricStmt = null;
    private PreparedStatement insertMetricStmt = null;
    private BatchedStatement updateMetricStmt = null;
    private BatchedStatement deleteMetricStmt = null;
    private BatchedStatement insertMetricValueStmt = null;
    private BatchedStatement updateMetricValueStmt = null;
    private PreparedStatement latestMetricDateStmt = null;
    private PreparedStatement checkMetricVersionStmt = null;
    private BatchedStatement insertMetricVersionStmt = null;
    private BatchedStatement insertMetricTargetStmt = null;
    private PreparedStatement checkSourceIdStmt = null;
    private BatchedStatement insertSourceIdStmt = null;
    private BatchedStatement updateSourceIdStmt = null;
    private PreparedStatement checkDefaultTargetStmt = null;
    private BatchedStatement insertDefaultTargetStmt = null;
    private HashMap<String, MetricName> nameCache = null;
    private HashSet<String> baseNameCache = null;
    
    public enum CheckResult {
        MISSING, DIFFERS, EXISTS
    }

    /**
     * Object describing a name of a metric as well as the components in it.
     */
    public static class MetricName {
        private final String name;
        private final String base_name;
        private final String domain_name;
        private final Integer id;
        
        /**
         * Create a metric name.
         * @param name The name of the metric, possibly including project-specific
         * domain names.
         */
        public MetricName(String name) {
            this(name, null, null, null);
        }
        
        /**
         * Create a metric name which has been split out into base name and domain name.
         * @param name The name of the metric, possibly including project-specific
         * domain names.
         * @param base_name The base name of the metric, shared with other projects.
         * @param domain_name The domain name of the metric, such as a project name,
         * team name, or product name.
         */
        public MetricName(String name, String base_name, String domain_name) {
            this(name, base_name, domain_name, null);
        }
        
        /**
         * Create a fully loaded metric name.
         * @param name The name of the metric, possibly including project-specific
         * domain names.
         * @param base_name The base name of the metric, shared with other projects.
         * @param domain_name The domain name of the metric, such as a project name,
         * team name, or product name.
         * @param id The identifier of the metric name in the database.
         */
        public MetricName(String name, String base_name, String domain_name, Integer id) {
            this.name = name;
            this.base_name = base_name;
            this.domain_name = domain_name;
            this.id = id;
        }
        
        public String getName() {
            return this.name;
        }
        
        public String getBaseName() {
            return this.base_name;
        }
        
        public String getDomainName() {
            return this.domain_name;
        }
        
        public Integer getId() {
            return this.id;
        }
        
        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (other instanceof MetricName) {
                MetricName otherName = (MetricName)other;
                return (name.equals(otherName.name) &&
                        (base_name == null ? otherName.base_name == null : base_name.equals(otherName.base_name)) &&
                        (domain_name == null ? otherName.domain_name == null : domain_name.equals(otherName.domain_name)) &&
                        (id == null || otherName.id == null || id.equals(otherName.id)));
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + Objects.hashCode(this.name);
            return hash;
        }
    }
    
    public MetricDb() {
        String sql = "UPDATE gros.metric SET name = ?, base_name = ?, domain_name = ? WHERE metric_id = ?";
        updateMetricStmt = new BatchedStatement(sql);
        sql = "DELETE FROM gros.metric WHERE metric_id = ?";
        deleteMetricStmt = new BatchedStatement(sql);
        sql = "insert into gros.metric_value(metric_id,value,category,date,sprint_id,since_date,project_id) values (?,?,?,?,?,?,?);";
        insertMetricValueStmt = new BatchedStatement(sql);
        sql = "update gros.metric_value set metric_id = ? where metric_id = ?";
        updateMetricValueStmt = new BatchedStatement(sql);
        sql = "insert into gros.metric_version(project_id,version_id,developer,message,commit_date,sprint_id) values (?,?,?,?,?,?);";
        insertMetricVersionStmt = new BatchedStatement(sql);
        sql = "insert into gros.metric_target(project_id,version_id,metric_id,type,target,low_target,comment) values (?,?,?,?,?,?,?);";
        insertMetricTargetStmt = new BatchedStatement(sql);
        sql = "insert into gros.source_id(project_id,domain_name,url,source_type,source_id) values (?,?,?,?,?);";
        insertSourceIdStmt = new BatchedStatement(sql);
        sql = "update gros.source_id set source_type = ?, source_id = ? where project_id = ? and domain_name = ? and url = ?";
        updateSourceIdStmt = new BatchedStatement(sql);
        sql = "insert into gros.metric_default(base_name,version_id,commit_date,direction,perfect,target,low_target) values (?,?,?,?,?,?,?);";
        insertDefaultTargetStmt = new BatchedStatement(sql);
    }
    
    private void getInsertMetricStmt() throws SQLException, PropertyVetoException {
        if (insertMetricStmt == null) {
            Connection con = insertMetricValueStmt.getConnection();
            String sql = "insert into gros.metric(name,base_name,domain_name) values (?,?,?)";
            insertMetricStmt = con.prepareStatement(sql);
        }
    }
    
    /**
     * Insert a metric name into the metrics table.
     * @param name Object describing the name of the metric, and possibly the
     * components in the name.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_metric(MetricName name) throws SQLException, PropertyVetoException {
        getInsertMetricStmt();
        
        insertMetricStmt.setString(1, name.getName());
        setString(insertMetricStmt, 2, name.getBaseName());
        setString(insertMetricStmt, 3, name.getDomainName());
        
        // Insert immediately because we need to have the row available for the identifier.
        insertMetricStmt.execute();
        if (name.getBaseName() != null) {
            baseNameCache.add(name.getBaseName());
        }
    }
    
    /**
     * Insert a metric measurement into the metric values table.
     * @param metric_id Identifier of the metric name
     * @param value The value of the metric at time of measurement
     * @param category The category related to the metric's value ('red', 'green', 'yellow')
     * @param date Timestamp at which the measurement took place
     * @param sprint_id Identifier of the sprint in which the measurement took place
     * @param since_date Timestamp since which the metric has the same value
     * @param project Identifier of the project in which the measurement was made
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_metricValue(int metric_id, float value, String category, Timestamp date, int sprint_id, Timestamp since_date, int project) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertMetricValueStmt.getPreparedStatement();
        
        pstmt.setInt(1, metric_id);
        pstmt.setFloat(2, value);
        pstmt.setString(3, category);
        pstmt.setTimestamp(4, date);
        pstmt.setInt(5, sprint_id);
        setTimestamp(pstmt, 6, since_date);
        pstmt.setInt(7, project);
                    
        insertMetricValueStmt.batch();
    }

    private void getLatestMetricDateStmt() throws SQLException, PropertyVetoException {
        if (latestMetricDateStmt == null) {
            Connection con = insertMetricValueStmt.getConnection();
            String sql = "select max(date) as latest_date from gros.metric_value where project_id=?";
            latestMetricDateStmt = con.prepareStatement(sql);
        }
    }
    
    /**
     * Retrieve the latest date within all metric value measurements for the
     * given project.
     * @param project_id Identifier of the project in which the measurements were made
     * @return A timestamp of the latest date, or null if there are no measurements
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public Timestamp get_latest_metric_date(int project_id) throws SQLException, PropertyVetoException {
        getLatestMetricDateStmt();
        
        latestMetricDateStmt.setInt(1, project_id);
        
        try (ResultSet rs = latestMetricDateStmt.executeQuery()) {
            if (rs.next()) {
                return rs.getTimestamp("latest_date");
            }
        }
        return null;
    }
    
    @Override
    public void close() throws SQLException {
        insertMetricValueStmt.execute();
        insertMetricValueStmt.close();
        
        updateMetricValueStmt.execute();
        updateMetricValueStmt.close();
        
        if (checkMetricStmt != null) {
            checkMetricStmt.close();
            checkMetricStmt = null;
        }
        
        if (insertMetricStmt != null) {
            insertMetricStmt.close();
            insertMetricStmt = null;
        }

        updateMetricStmt.execute();
        updateMetricStmt.close();

        deleteMetricStmt.execute();
        deleteMetricStmt.close();
        
        if (checkMetricVersionStmt != null) {
            checkMetricVersionStmt.close();
            checkMetricVersionStmt = null;
        }
        insertMetricVersionStmt.execute();
        insertMetricVersionStmt.close();
        
        insertMetricTargetStmt.execute();
        insertMetricTargetStmt.close();
        
        if (checkSourceIdStmt != null) {
            checkSourceIdStmt.close();
            checkSourceIdStmt = null;
        }
        
        insertSourceIdStmt.execute();
        insertSourceIdStmt.close();
        
        updateSourceIdStmt.execute();
        updateSourceIdStmt.close();
        
        if (checkDefaultTargetStmt != null) {
            checkDefaultTargetStmt.close();
            checkDefaultTargetStmt = null;
        }
        
        insertDefaultTargetStmt.execute();
        insertDefaultTargetStmt.close();

        clearCaches();
    }
    
    private void getCheckMetricStmt() throws SQLException, PropertyVetoException {
        if (checkMetricStmt == null) {
            Connection con = insertMetricValueStmt.getConnection();
            String sql = "SELECT metric_id, name, base_name, domain_name FROM gros.metric WHERE UPPER(name) = ?";
            checkMetricStmt = con.prepareStatement(sql);
        }
    }
    
    private void clearCaches() {
        if (nameCache != null) {
            nameCache.clear();
            nameCache = null;
        }
        if (baseNameCache != null) {
            baseNameCache.clear();
            baseNameCache = null;
        }
    }

    private static String caseFold(String name) {
        return name.toUpperCase().trim();
    }
    
    private void fillNameCache() throws SQLException, PropertyVetoException {
        if (nameCache != null) {
            return;
        }
        nameCache = new HashMap<>();
        baseNameCache = new HashSet<>();
        
        Connection con = insertMetricValueStmt.getConnection();
        String sql = "SELECT name, base_name, domain_name, metric_id FROM gros.metric";
        
        try (
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql)
        ) {
            while(rs.next()) {
                String name = rs.getString("name");
                String base_name = rs.getString("base_name");
                String domain_name = rs.getString("domain_name");
                Integer id = Integer.parseInt(rs.getString("metric_id"));
                nameCache.put(caseFold(name), new MetricName(name, base_name, domain_name, id));
                if (base_name != null) {
                    baseNameCache.add(base_name);
                }
            }
        }
    }
    
    /**
     * Load a collection of base names into the base name cache.
     * This is only used for matching base and domain names from metric names,
     * and is not stored as is in the database. The cache is loaded based on
     * existing database data and the base name cache.
     * @param base_names The collection of base names to load
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void load_metric_base_names(Collection<String> base_names) throws SQLException, PropertyVetoException {
        fillNameCache();
        
        baseNameCache.addAll(base_names);
    }
    
    /**
     * Check whether a metric name exists in the cache and return its data.
     * This method does not check the database except for initial population of
     * the cache, thus the cache may grow stale if new metrics are added within
     * the process.
     * @param name Name of the metric
     * @return The metric name with identifier, or null if it is not found
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public MetricName check_metric(String name) throws SQLException, PropertyVetoException {
        return check_metric(name, false);
    }
    
    /**
     * Check whether a metric name exists in the cache or database and return
     * its data.
     * @param name Name of the metric
     * @param recache Whether to retrieve from the database. If true, then the
     * cache is checked first for the case-folded metric name, or if not found,
     * the case-folded metric name is searched in the database and the cache is
     * updated with a found metric ID. If false, only the cache is checked for
     * the case-folded metric name.
     * @return The metric name with identifier, or null if it is not found
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public MetricName check_metric(String name, boolean recache) throws SQLException, PropertyVetoException {
        fillNameCache();
        
        String key = caseFold(name);
        MetricName found = nameCache.get(key);
        if (found != null) {
            return found;
        }

        if (recache) {
            getCheckMetricStmt();

            checkMetricStmt.setString(1, key);
            try (ResultSet rs = checkMetricStmt.executeQuery()) {
                if (rs.next()) {
                    found = new MetricName(rs.getString("name"), rs.getString("base_name"), rs.getString("domain_name"), rs.getInt("metric_id"));
                }
            }

            nameCache.put(key, found);
        }
        
        return found;
    }
    
    /**
     * Update a metric to refer to a different name.
     * @param metric_id The internal identifier of the metric to update.
     * @param old_name The old name of the metric
     * @param name Object describing the new name of the metric, and possibly the
     * components in the name. The metric ID in the object is ignored.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void update_metric(int metric_id, String old_name, MetricName name) throws SQLException, PropertyVetoException {
        if (nameCache != null) {
            // Safety check based on cache
            String key = caseFold(old_name);
            MetricName cacheName = nameCache.get(key);
            if (cacheName != null && cacheName.getId() != metric_id) {
                throw new IllegalArgumentException("Incorrect metric ID provided");
            }
            // Update the name cache to use the new name to refer to the metric ID
            nameCache.put(key, null);
            nameCache.put(caseFold(name.getName()), new MetricName(name.getName(), name.getBaseName(), name.getDomainName(), metric_id));
        }
        
        PreparedStatement pstmt = updateMetricStmt.getPreparedStatement();
        pstmt.setString(1, name.getName());
        setString(pstmt, 2, name.getBaseName());
        setString(pstmt, 3, name.getDomainName());
        pstmt.setInt(4, metric_id);
        
        updateMetricStmt.batch();
    }
    
    /**
     * Delete a metric by removing the name from the metrics table.
     * This also updates the metric values to refer to another metric.
     * Note that metric targets are not updated.
     * @param metric_id The internal identifier of the metric.
     * @param old_name The name of the metric that is removed.
     * @param other_id The internal identifier of the metric to point values to.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void delete_metric(int metric_id, String old_name, int other_id) throws SQLException, PropertyVetoException {
        if (nameCache != null) {
            // Mark the name as removed in the cache, but only if it refers to this metric ID
            String key = caseFold(old_name);
            if (!nameCache.containsKey(key) || nameCache.get(key).getId() == metric_id) {
                nameCache.put(key, null);
            }
        }
        
        PreparedStatement pstmt = deleteMetricStmt.getPreparedStatement();
        pstmt.setInt(1, metric_id);
        deleteMetricStmt.batch();
        
        // Refer to other metric in metric values.
        pstmt = updateMetricValueStmt.getPreparedStatement();
        pstmt.setInt(1, other_id);
        pstmt.setInt(2, metric_id);
        updateMetricValueStmt.batch();
    }
    
    /**
     * Split a metric name into an object describing its components.
     * @param metric_name Name of the metric
     * @param aggressive Whether to aggressively search for components
     * @return An object describing the metric name, and the components found in
     * the name if possible. The metric name itself may be changed as well.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public MetricName split_metric_name(String metric_name, boolean aggressive) throws SQLException, PropertyVetoException {
        fillNameCache();
        
        metric_name = metric_name.replaceFirst("<[A-Za-z.]+?([^.]+)(?: object at .*)>$", "$1");
        String base_name = metric_name;
        StringBuilder domain_name = new StringBuilder();
        Pattern pattern = Pattern.compile(aggressive ? ".$" : "(?<![A-Z ])[A-Z]+[^A-Z ]*(?: .+)?$");
        while (!baseNameCache.contains(base_name)) {
            Matcher matcher = pattern.matcher(base_name);
            if (matcher.find()) {
                String domain_part = matcher.group();
                if (domain_part.isEmpty()) {
                    // Match went wrong
                    return new MetricName(metric_name);
                }
                String new_base_name = matcher.replaceFirst("");
                if (base_name.equals(new_base_name)) {
                    // Cannot be split any further
                    return new MetricName(metric_name);
                }
                base_name = new_base_name;
                domain_name.insert(0, domain_part);
            }
            else {
                // No more match, so bail out
                return new MetricName(metric_name);
            }
        }
        return new MetricName(metric_name, base_name, domain_name.toString());
    }

    private void getCheckVersionStmt() throws SQLException, PropertyVetoException {
        if (checkMetricVersionStmt == null) {
            Connection con = insertMetricValueStmt.getConnection();
            String sql = "SELECT version_id FROM gros.metric_version WHERE project_id = ? AND version_id = ?";
            checkMetricVersionStmt = con.prepareStatement(sql);
        }
    }
    
    /**
     * Check whether a metric version exists in the database.
     * @param projectId Identifier of the project to which the metric version change applies
     * @param version The version identifier of the change
     * @return The revision identifier if the version exists, or null if it is not found
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public String check_version(int projectId, String version) throws SQLException, PropertyVetoException {
        getCheckVersionStmt();
        String idVersion = null;
        
        checkMetricVersionStmt.setInt(1, projectId);
        checkMetricVersionStmt.setString(2, version);
        try (ResultSet rs = checkMetricVersionStmt.executeQuery()) {
            while (rs.next()) {
                idVersion = rs.getString("version_id");
            }
        }
        
        return idVersion;
    }

    /**
     * Insert a new metric version in the database
     * @param projectId Identifier of the project to which the metric version change applies
     * @param version The version identifier of the change
     * @param developer Shorthand name of the developer that made the change
     * @param message Commit message describing the change 
     * @param commit_date Timestamp at which the target change took place
     * @param sprint_id Identifier of the sprint in which the target norms were changed
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_version(int projectId, String version, String developer, String message, Timestamp commit_date, int sprint_id) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertMetricVersionStmt.getPreparedStatement();
        
        pstmt.setInt(1, projectId);
        pstmt.setString(2, version);
        pstmt.setString(3, developer);
        pstmt.setString(4, message);
        pstmt.setTimestamp(5, commit_date);
        pstmt.setInt(6, sprint_id);
                    
        insertMetricVersionStmt.batch();
    }

    /**
     * Insert a new project-specific target norm change in the metric targets table.
     * @param projectId Identifier of the project to which the metric norm change applies
     * @param version The version identifier of the change
     * @param metricId Identifier of the metric name
     * @param type The type of norm change: 'options', 'old_options', 'TechnicalDebtTarget' or 'DynamicTechnicalDebtTarget'
     * @param target The norm value at which the category changes from green to yellow
     * @param low_target The norm value at which the category changes from yellow to red
     * @param comment Comment for technical debt targets describing the reason of the norm change
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_target(int projectId, String version, int metricId, String type, int target, int low_target, String comment) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertMetricTargetStmt.getPreparedStatement();
        
        pstmt.setInt(1, projectId);
        pstmt.setString(2, version);
        pstmt.setInt(3, metricId);
        pstmt.setString(4, type);
        pstmt.setInt(5, target);
        pstmt.setInt(6, low_target);
        pstmt.setString(7, comment);
        
        insertMetricTargetStmt.batch();
    }
        
    private void getCheckSourceIdStmt() throws SQLException, PropertyVetoException {
        if (checkSourceIdStmt == null) {
            Connection con = insertSourceIdStmt.getConnection();
            String sql = "SELECT source_type, source_id FROM gros.source_id WHERE project_id = ? AND domain_name = ? AND url = ?";
            checkSourceIdStmt = con.prepareStatement(sql);
        }
    }
    
    /**
     * Check whether the given domain name in the project's metric source has
     * the given source ID in the database.
     * @param projectId Identifier of the project in which the domain name exists.
     * @param domain_name The name of the object that is being measured.
     * @param url The URL of the source at which the source ID may be used to
     * identify the domain object.
     * @param source_type The type of the source.
     * @param source_id The identifier of the domain object at the source.
     * @return An indicator of the state of the database regarding the source ID.
     * This is CheckResult.MISSING if the source ID for the provided project,
     * domain name and URL does not exist. This is CheckResult.DIFFERS if there
     * is a row for the provided domain name in the database, but it has
     * a different source type or ID in its fields. This is CheckResult.EXISTS
     * if there is a source ID in the database that matches all the provided
     * parameters.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public CheckResult check_source_id(int projectId, String domain_name, String url, String source_type, String source_id) throws SQLException, PropertyVetoException {
        getCheckSourceIdStmt();
        
        checkSourceIdStmt.setInt(1, projectId);
        checkSourceIdStmt.setString(2, domain_name);
        checkSourceIdStmt.setString(3, url);
        
        try (ResultSet rs = checkSourceIdStmt.executeQuery()) {
            while (rs.next()) {
                if ((source_type == null ? rs.getObject("source_type") == null : source_type.equals(rs.getString("source_type"))) &&
                    source_id.equals(rs.getString("source_id"))) {
                    return CheckResult.EXISTS;
                }
                return CheckResult.DIFFERS;
            }
        }
        
        return CheckResult.MISSING;
    }
    
    /**
     * Insert a source ID for the given domain name for the project's metric source.
     * @param projectId Identifier of the project in which the domain name exists.
     * @param domain_name The name of the object that is being measured.
     * @param url The URL of the source at which the source ID may be used to
     * identify the domain object.
     * @param source_type The type of the source.
     * @param source_id The identifier of the domain object at the source.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_source_id(int projectId, String domain_name, String url, String source_type, String source_id) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertSourceIdStmt.getPreparedStatement();
        
        pstmt.setInt(1, projectId);
        pstmt.setString(2, domain_name);
        pstmt.setString(3, url);
        setString(pstmt, 4, source_type);
        pstmt.setString(5, source_id);
        
        insertSourceIdStmt.batch();
    }

    /**
     * Update a source ID for the given domain name for the project's metric source.
     * @param projectId Identifier of the project in which the domain name exists.
     * @param domain_name The name of the object that is being measured.
     * @param url The URL of the source at which the source ID may be used to
     * identify the domain object.
     * @param source_type The type of the source.
     * @param source_id The identifier of the domain object at the source.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void update_source_id(int projectId, String domain_name, String url, String source_type, String source_id) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = updateSourceIdStmt.getPreparedStatement();
        
        setString(pstmt, 1, source_type);
        pstmt.setString(2, source_id);
        pstmt.setInt(3, projectId);
        pstmt.setString(4, domain_name);
        pstmt.setString(5, url);
        
        updateSourceIdStmt.batch();
    }

    private void getCheckDefaultTargetStmt() throws SQLException, PropertyVetoException {
        if (checkDefaultTargetStmt == null) {
            Connection con = insertDefaultTargetStmt.getConnection();
            String sql = "SELECT base_name FROM gros.metric_default WHERE base_name = ? AND version_id = ?";
            checkDefaultTargetStmt = con.prepareStatement(sql);
        }
    }
    
    /**
     * Check whether the given metric base name has a default target value for the revision.
     * @param base_name The base name of the metric that has a default target value.
     * @param version The revision in which the default target value applies to the metric.
     * @return Whether the metric name has a default target value at the revision.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public boolean check_default_target(String base_name, String version) throws SQLException, PropertyVetoException {
        getCheckDefaultTargetStmt();
        
        checkDefaultTargetStmt.setString(1, base_name);
        checkDefaultTargetStmt.setString(2, version);
        
        try (ResultSet rs = checkDefaultTargetStmt.executeQuery()) {
            if (rs.next()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Insert default target values at a revision for the given metric base name.
     * @param base_name The base name of the metric that has a default target value.
     * @param version The revision in which the default target value applies to the metric.
     * @param commit_date The commit date of the revision.
     * @param direction Whether the metric improves if the metric value increases.
     * This is true if a higher value is better, false if a lower value is better,
     * or null if it is not known or not applicable for this metric.
     * @param perfect The perfect value of the metric, or null if it is not known.
     * @param target The target value of the metric, or null if it is not known.
     * @param low_target The low target value of the metric, or null if it is not known.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_default_target(String base_name, String version, Timestamp commit_date, Boolean direction, Float perfect, Float target, Float low_target) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertDefaultTargetStmt.getPreparedStatement();
        
        pstmt.setString(1, base_name);
        pstmt.setString(2, version);
        pstmt.setTimestamp(3, commit_date);
        setBoolean(pstmt, 4, direction);
        setFloat(pstmt, 5, perfect);
        setFloat(pstmt, 6, target);
        setFloat(pstmt, 7, low_target);
        
        insertDefaultTargetStmt.batch();
    }

}
    
