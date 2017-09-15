/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
    private HashMap<String, Integer> nameCache = null;
    private HashSet<String> baseNameCache = null;
    
    /**
     * Object describing a name of a metric as well as the components in it.
     */
    public static class MetricName {
        private final String name;
        private final String base_name;
        private final String domain_name;
        
        /**
         * Create a metric name.
         * @param name The name of the metric, possibly including project-specific
         * domain names.
         */
        public MetricName(String name) {
            this(name, null, null);
        }
        
        /**
         * Create a fully loaded metric name.
         * @param name The name of the metric, possibly including project-specific
         * domain names.
         * @param base_name The base name of the metric, shared with other projects.
         * @param domain_name The domain name of the metric, such as a project name,
         * team name, or product name.
         */
        public MetricName(String name, String base_name, String domain_name) {
            this.name = name;
            this.base_name = base_name;
            this.domain_name = domain_name;
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

        clearCaches();
    }
    
    private void getCheckMetricStmt() throws SQLException, PropertyVetoException {
        if (checkMetricStmt == null) {
            Connection con = insertMetricValueStmt.getConnection();
            String sql = "SELECT metric_id FROM gros.metric WHERE UPPER(name) = ?";
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
        String sql = "SELECT UPPER(name) AS key, base_name, metric_id FROM gros.metric";
        
        try (
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql)
        ) {
            while(rs.next()) {
                String key = rs.getString("key");
                String base_name = rs.getString("base_name");
                Integer id = Integer.parseInt(rs.getString("metric_id"));
                nameCache.put(key, id);
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
     * Check whether a metric name exists in the cache and return its identifier.
     * This method does not check the database except for initial population of
     * the cache, thus the cache may grow stale if new metrics are added within
     * the process.
     * @param name Name of the metric
     * @return Identifier of the metric, or 0 if it is not found
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public int check_metric(String name) throws SQLException, PropertyVetoException {
        return check_metric(name, false);
    }
    
    /**
     * Check whether a metric name exists in the cache or database and return
     * its identifier.
     * @param name Name of the metric
     * @param recache Whether to retrieve from the database. If true, then the
     * cache is checked first for the case-folded metric name, or if not found,
     * the case-folded metric name is searched in the database and the cache is
     * updated with a found metric ID. If false, only the cache is checked for
     * the case-folded metric name.
     * @return Identifier of the metric, or 0 if it is not found
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public int check_metric(String name, boolean recache) throws SQLException, PropertyVetoException {
        fillNameCache();
        
        String key = caseFold(name);
        Integer cacheId = nameCache.get(key);
        if (cacheId != null) {
            return cacheId;
        }

        Integer idMetric = null;
        if (recache) {
            getCheckMetricStmt();

            checkMetricStmt.setString(1, key);
            try (ResultSet rs = checkMetricStmt.executeQuery()) {
                while (rs.next()) {
                    idMetric = rs.getInt("metric_id");
                }
            }

            nameCache.put(key, idMetric);
        }
        
        if (idMetric == null) {
            return 0;
        }
        
        return idMetric;
    }
    
    /**
     * Update a metric to refer to a different name.
     * @param metric_id The internal identifier of the metric.
     * @param old_name The old name of the metric
     * @param name Object describing the new name of the metric, and possibly the
     * components in the name.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void update_metric(int metric_id, String old_name, MetricName name) throws SQLException, PropertyVetoException {
        if (nameCache != null) {
            // Safety check based on cache
            String key = caseFold(old_name);
            Integer cacheId = nameCache.get(key);
            if (cacheId != null && cacheId != metric_id) {
                throw new IllegalArgumentException("Incorrect metric ID provided");
            }
            // Update the name cache to use the new name to refer to the metric ID
            nameCache.put(key, 0);
            nameCache.put(caseFold(name.getName()), metric_id);
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
            if (!nameCache.containsKey(key) || nameCache.get(key) == metric_id) {
                nameCache.put(key, 0);
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
        String domain_name = "";
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
                domain_name = domain_part + domain_name;
            }
            else {
                // No more match, so bail out
                return new MetricName(metric_name);
            }
        }
        return new MetricName(metric_name, base_name, domain_name);
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
     * @return The revision number if the version exists, or 0 if it is not found
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public int check_version(int projectId, String version) throws SQLException, PropertyVetoException {
        getCheckVersionStmt();
        int idVersion = 0;
        
        checkMetricVersionStmt.setInt(1, projectId);
        checkMetricVersionStmt.setString(2, version);
        try (ResultSet rs = checkMetricVersionStmt.executeQuery()) {
            while (rs.next()) {
                idVersion = rs.getInt("version_id");
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
        

}
    
