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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;
import util.BaseDb;
import util.Bisect;

/**
 * Database access magement for sprint properties.
 * @author Leon Helwerda
 */
public class SprintDb extends BaseDb implements AutoCloseable {
    private BatchedStatement insertStmt = null;
    private PreparedStatement checkStmt = null;
    private BatchedStatement updateStmt = null;
    private PreparedStatement cacheStmt = null;
    private final HashMap<Integer, HashMap<Integer, Sprint>> keyCache;
    private final HashMap<Integer, Sprint[]> dateCache;
    
    private static class Sprint implements Comparable<Timestamp> {
        private final int sprint_id;
        private final String name;
        private final Timestamp start_date;
        private final Timestamp end_date;
        private final Timestamp complete_date;
        
        public Sprint(int sprint_id, String name, Timestamp start_date, Timestamp end_date, Timestamp complete_date) {
            this.sprint_id = sprint_id;
            this.name = name;
            this.start_date = start_date;
            this.end_date = end_date;
            this.complete_date = complete_date;
        }

        public int getSprintId() {
            return this.sprint_id;
        }
        
        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (other instanceof Sprint) {
                Sprint otherSprint = (Sprint)other;
                return (sprint_id == otherSprint.sprint_id &&
                        name.equals(otherSprint.name) &&
                        (start_date == null ? otherSprint.start_date == null : start_date.equals(otherSprint.start_date)) &&
                        (end_date == null ? otherSprint.end_date == null : end_date.equals(otherSprint.end_date)) &&
                        (complete_date == null ? otherSprint.complete_date == null : complete_date.equals(otherSprint.complete_date)));
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + this.sprint_id;
            hash = 29 * hash + Objects.hashCode(this.start_date);
            hash = 29 * hash + Objects.hashCode(this.end_date);
            hash = 29 * hash + Objects.hashCode(this.complete_date);
            return hash;
        }
        
        @Override
        public int compareTo(Timestamp other) {
            return start_date.compareTo(other);
        }
        
        public static Comparator<Sprint> getComparator() {
            return new Comparator<Sprint>() {
                @Override
                public int compare(Sprint first, Sprint other) {
                    return Integer.compare(first.sprint_id, other.sprint_id);
                }
            };
        }
        
        public boolean contains(Timestamp date) {
            if (this.start_date == null || date.before(this.start_date)) {
                return false;
            }
            else if (this.end_date != null && date.after(this.end_date)) {
                return false;
            }
            else if (this.complete_date != null && date.after(this.complete_date)) {
                return false;
            }
            
            return true;
        }
    }

    public enum CheckResult {
        MISSING, DIFFERS, EXISTS
    };

    public SprintDb() {
        String sql = "insert into gros.sprint(sprint_id,project_id,name,start_date,end_date,complete_date) values (?,?,?,?,?,?);";
        insertStmt = new BatchedStatement(sql);
        
        sql = "update gros.sprint set name=?, start_date=?, end_date=?, complete_date=? where sprint_id=? and project_id=?;";
        updateStmt = new BatchedStatement(sql);
        
        keyCache = new HashMap<>();
        dateCache = new HashMap<>();
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
        fillCache(project_id);
        if (!keyCache.containsKey(project_id)) {
            return CheckResult.MISSING;
        }
        HashMap<Integer, Sprint> sprints = keyCache.get(project_id);
        Sprint currentSprint = sprints.get(sprint_id);
        if (currentSprint != null) {
            Sprint sprint = new Sprint(sprint_id, name, start_date, end_date, complete_date);
            if (currentSprint.equals(sprint)) {
                return CheckResult.EXISTS;
            }
            else {
                return CheckResult.DIFFERS;
            }
        }
        else {
            return CheckResult.MISSING;
        }
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
    
    private void fillCache(int project_id) throws SQLException, IOException, PropertyVetoException {
        if (keyCache.containsKey(project_id)) {
            return;
        }
        
        if (cacheStmt == null) {
            Connection con = insertStmt.getConnection();
            String sql = "SELECT sprint_id, name, start_date, end_date, complete_date FROM gros.sprint WHERE project_id = ? ORDER BY start_date";
            cacheStmt = con.prepareStatement(sql);
        }
        
        HashMap<Integer, Sprint> sprints = new HashMap<>();
        ArrayList<Sprint> dateSprints = new ArrayList<>();
        cacheStmt.setInt(1, project_id);
        try (ResultSet rs = cacheStmt.executeQuery()) {
            while (rs.next()) {
                int sprint_id = rs.getInt("sprint_id");
                String name = rs.getString("name");
                Timestamp start_date = rs.getTimestamp("start_date");
                Timestamp end_date = rs.getTimestamp("end_date");
                Timestamp complete_date = rs.getTimestamp("complete_date");
                Sprint sprint = new Sprint(sprint_id, name, start_date, end_date, complete_date);
                sprints.put(sprint_id, sprint);
                dateSprints.add(sprint);
           }
        }
        keyCache.put(project_id, sprints);
        dateCache.put(project_id, dateSprints.toArray(new Sprint[dateSprints.size()]));
        
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
        fillCache(project_id);
        Sprint[] sprints = dateCache.get(project_id);
        
        int index = Bisect.bisectRight(sprints, date);
        if (index == 0) {
            // Older than all sprints
            return 0;
        }
        if (!sprints[index-1].contains(date)) {
            if (index > 1 && sprints[index-2].contains(date)) {
                return sprints[index-2].getSprintId();
            }
            else {
                return 0;
            }
        }
        
        return sprints[index-1].getSprintId();
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

        for (HashMap<Integer, Sprint> cache : keyCache.values()) {
            cache.clear();
        }
        keyCache.clear();
        dateCache.clear();
    }
    
}
