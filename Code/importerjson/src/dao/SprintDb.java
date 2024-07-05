/**
 * Primary source sprint tables.
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import util.BaseDb;
import util.Bisect;

/**
 * Database access magement for Jira and TFS sprint properties.
 * @author Leon Helwerda
 */
public class SprintDb extends BaseDb implements AutoCloseable {
    private BatchedStatement insertJiraStmt = null;
    private BatchedStatement updateJiraStmt = null;
    private PreparedStatement cacheJiraStmt = null;

    private BatchedStatement insertTfsStmt = null;
    private PreparedStatement checkTfsStmt = null;
   
    private final HashMap<Integer, HashMap<Integer, JiraSprint>> keyCache;
    private final HashMap<Integer, JiraSprint[]> dateCache;

    /**
     * A sprint object. The sprint contains properties extracted from JIRA,
     * including an internal identifier, the human-readable name, and date ranges.
     */
    private static class Sprint implements Comparable<Timestamp> {
        protected final int sprint_id;
        protected final String name;
        protected final Timestamp start_date;
        protected final Timestamp end_date;
        
        public Sprint(int sprint_id, String name, Timestamp start_date, Timestamp end_date) {
            this.sprint_id = sprint_id;
            this.name = name;
            this.start_date = start_date;
            this.end_date = end_date;            
        }
        
        /**
         * Retrieve the identifier of this sprint. This identifier is unique
         * within the project the sprint is in.
         * @return The sprint's internal identifier
         */
        public int getSprintId() {
            return this.sprint_id;
        }
        
        /**
         * Check whether a sprint is equal to another. This does not include any
         * checks whether the sprints are in the same project.
         * @param other The other object
         * @return Whether the other object is a Sprint with the same properties
         */
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
                        (end_date == null ? otherSprint.end_date == null : end_date.equals(otherSprint.end_date)));
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 79 * hash + this.sprint_id;
            hash = 79 * hash + Objects.hashCode(this.name);
            hash = 79 * hash + Objects.hashCode(this.start_date);
            hash = 79 * hash + Objects.hashCode(this.end_date);
            return hash;
        }

        /**
         * Compare the start date of the sprint to another timestamp.
         * This can be used for sorting multiple sprints on their start date, or
         * a first step in finding the sprint that encompasses a given date.
         * @param other A Timestamp of a moment in time to compare to the sprint
         * @return the value 0 if the start date is equal to the given timestamp;
         * a value less than 0 if the start date is before the given timestamp;
         * and a value greater than 0 if the start date is after the given timestamp.
         */
        @Override
        public int compareTo(Timestamp other) {
            // Unknown start dates are always later than the timestamp.
            if (start_date == null) {
                return 1;
            }
            return start_date.compareTo(other);
        }
        
        /**
         * Provide a comparator between Sprint objects.
         * This comparator uses only the internal sprint ID to compare the sprints
         * for sorting.
         * @return A custom comparator object
         */
        public static Comparator<Sprint> getComparator() {
            return new Comparator<Sprint>() {
                @Override
                public int compare(Sprint first, Sprint other) {
                    return Integer.compare(first.sprint_id, other.sprint_id);
                }
            };
        }
        
        /**
         * Check whether the given date is encompassed by this sprint.
         * For a sprint to contain a certain date, it must have a start date
         * that is earlier than or at the same moment as the given date,
         * its end date (if provided) must be later than or at the same moment
         * as the given date, and the complete date (if provided) must also be
         * later than or at the same moment as the given date.
         * @param date The date to check
         * @return Whether the date is in the sprint's date range
         */
        public boolean contains(Timestamp date) {
            return (
                (this.start_date != null && date.after(this.start_date)) &&
                (this.end_date == null || date.before(this.end_date))
            );
        }
    }
    
    private final static class JiraSprint extends Sprint {
        private final Timestamp complete_date;
        private final String goal;
        private final Integer board_id;
        
        public JiraSprint(int sprint_id, String name, Timestamp start_date, Timestamp end_date, Timestamp complete_date, String goal, Integer board_id) {
            super(sprint_id, name, start_date, end_date);
            this.complete_date = complete_date;
            this.goal = goal;
            this.board_id = board_id;
        }
        
        @Override
        public boolean equals(Object other) {
            boolean equal = super.equals(other);
            if (equal && other instanceof JiraSprint) {
                JiraSprint otherSprint = (JiraSprint)other;
                return ((complete_date == null ? otherSprint.complete_date == null : complete_date.equals(otherSprint.complete_date)) &&
                        (goal == null ? otherSprint.goal == null : goal.equals(otherSprint.goal)) &&
                        Objects.equals(board_id, otherSprint.board_id));

            }
            return equal;
        }

        @Override
        public int hashCode() {
            int hash = super.hashCode();
            hash = 79 * hash + Objects.hashCode(this.complete_date);
            hash = 79 * hash + Objects.hashCode(this.goal);
            hash = 79 * hash + Objects.hashCode(this.board_id);
            return hash;
        }
        
        @Override
        public boolean contains(Timestamp date) {
            return super.contains(date) &&
                (this.complete_date == null || date.before(this.complete_date));
        }        
    }
    
    private final static class TfsSprint extends Sprint {
        private final Integer repo_id;
        private final Integer team_id;
        
        public TfsSprint(int sprint_id, String name, Timestamp start_date, Timestamp end_date, Integer repo_id, Integer team_id) {
            super(sprint_id, name, start_date, end_date);
            this.repo_id = repo_id;
            this.team_id = team_id;
        }
        
        @Override
        public boolean equals(Object other) {
            boolean equal = super.equals(other);
            if (equal && other instanceof TfsSprint) {
                TfsSprint otherSprint = (TfsSprint)other;
                return ((repo_id == null ? otherSprint.repo_id == null : repo_id.equals(otherSprint.repo_id)) &&
                        (team_id == null ? otherSprint.team_id == null : team_id.equals(otherSprint.team_id)));
            }
            return equal;
        }

        @Override
        public int hashCode() {
            int hash = super.hashCode();
            hash = 79 * hash + Objects.hashCode(this.repo_id);
            hash = 79 * hash + Objects.hashCode(this.team_id);
            return hash;
        }
    }

    public enum CheckResult {
        MISSING, DIFFERS, EXISTS
    }

    public SprintDb() {
        String sql = "insert into gros.sprint(sprint_id,project_id,name,start_date,end_date,complete_date,goal,board_id) values (?,?,?,?,?,?,?,?);";
        insertJiraStmt = new BatchedStatement(sql);
        
        sql = "update gros.sprint set name=?, start_date=?, end_date=?, complete_date=?, goal=?, board_id=? where sprint_id=? and project_id=?;";
        updateJiraStmt = new BatchedStatement(sql);
        
        sql = "insert into gros.tfs_sprint(project_id,name,start_date,end_date,repo_id,team_id) values (?,?,?,?,?,?);";
        insertTfsStmt = new BatchedStatement(sql);
        
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
     * @param goal The goal set for the sprint or null if the sprint has no goal
     * @param board_id The primary board ID on which the sprint is tracked.
     * @return The check result: CheckResult.MISSING if the sprint and project ID
     * combination is not found, CheckResult.EXISTS if all properties match, or
     * CheckResult.DIFFERS if the sprint and project ID is found but the name and/or
     * dates do not match.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public CheckResult check_sprint(int sprint_id, int project_id, String name, Timestamp start_date, Timestamp end_date, Timestamp complete_date, String goal, Integer board_id) throws SQLException, PropertyVetoException {
        fillCache(project_id);
        if (!keyCache.containsKey(project_id)) {
            return CheckResult.MISSING;
        }
        HashMap<Integer, JiraSprint> sprints = keyCache.get(project_id);
        JiraSprint currentSprint = sprints.get(sprint_id);
        if (currentSprint != null) {
            JiraSprint sprint = new JiraSprint(sprint_id, name, start_date, end_date, complete_date, goal, board_id);
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
     * @param goal The goal set for the sprint or null if the sprint has no goal
     * @param board_id The primary board ID on which the sprint is tracked.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_sprint(int sprint_id, int project_id, String name, Timestamp start_date, Timestamp end_date, Timestamp complete_date, String goal, Integer board_id) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertJiraStmt.getPreparedStatement();
        pstmt.setInt(1, sprint_id);
        pstmt.setInt(2, project_id);
        pstmt.setString(3, name);
        setTimestamp(pstmt, 4, start_date);
        setTimestamp(pstmt, 5, end_date);
        setTimestamp(pstmt, 6, complete_date);
        setString(pstmt, 7, goal);
        setInteger(pstmt, 8, board_id);

        insertJiraStmt.batch();
    }
    
    /**
     * Update an existing sprint in the database to hold different properties.
     * @param sprint_id The internal sprint ID from JIRA.
     * @param project_id The project identifier.
     * @param name The human-readable name of the sprint as provided in JIRA.
     * @param start_date The date at which the sprint starts or is set to start.
     * @param end_date The date at which the sprint ends or is set to end.
     * @param complete_date The date at which the tasks in the sprint are completed.
     * @param goal The goal set for the sprint or null if the sprint has no goal
     * @param board_id The primary board ID on which the sprint is tracked.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void update_sprint(int sprint_id, int project_id, String name, Timestamp start_date, Timestamp end_date, Timestamp complete_date, String goal, Integer board_id) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = updateJiraStmt.getPreparedStatement();
        pstmt.setString(1, name);
        setTimestamp(pstmt, 2, start_date);
        setTimestamp(pstmt, 3, end_date);
        setTimestamp(pstmt, 4, complete_date);
        setString(pstmt, 5, goal);
        setInteger(pstmt, 6, board_id);
        
        pstmt.setInt(7, sprint_id);
        pstmt.setInt(8, project_id);
        
        updateJiraStmt.batch();
    }
    
    private void fillCache(int project_id) throws SQLException, PropertyVetoException {
        if (keyCache.containsKey(project_id)) {
            return;
        }
        
        if (cacheJiraStmt == null) {
            Connection con = insertJiraStmt.getConnection();
            String sql = "SELECT sprint_id, name, start_date, end_date, complete_date, goal, board_id FROM gros.sprint WHERE project_id = ? ORDER BY start_date";
            cacheJiraStmt = con.prepareStatement(sql);
        }
        
        HashMap<Integer, JiraSprint> sprints = new HashMap<>();
        ArrayList<JiraSprint> dateSprints = new ArrayList<>();
        cacheJiraStmt.setInt(1, project_id);
        try (ResultSet rs = cacheJiraStmt.executeQuery()) {
            while (rs.next()) {
                int sprint_id = rs.getInt("sprint_id");
                String name = rs.getString("name");
                Timestamp start_date = rs.getTimestamp("start_date");
                Timestamp end_date = rs.getTimestamp("end_date");
                Timestamp complete_date = rs.getTimestamp("complete_date");
                String goal = rs.getString("goal");
                Integer board_id = rs.getObject("board_id") == null ? null : rs.getInt("board_id");
                JiraSprint sprint = new JiraSprint(sprint_id, name, start_date, end_date, complete_date, goal, board_id);
                sprints.put(sprint_id, sprint);
                dateSprints.add(sprint);
           }
        }
        keyCache.put(project_id, sprints);
        dateCache.put(project_id, dateSprints.toArray(new JiraSprint[dateSprints.size()]));
        
    }
    
    /**
     * Find a sprint for the given project that contains the provided date.
     * @param project_id The project identifier to use sprints from.
     * @param date The date to contain in the sprint.
     * @return The sprint ID of the latest sprint that contains the date, or 0
     * if there are no sprints that contain the date.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public int find_sprint(int project_id, Timestamp date) throws SQLException, PropertyVetoException {
        if (date == null) {
            // Unknown timestamps cannot be matched to a specific sprint
            return 0;
        }
        
        fillCache(project_id);
        JiraSprint[] sprints = dateCache.get(project_id);
        
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
    
    /**
     * Find sprints for the given project that have dates that overlap with the
     * given time range.
     * @param project_id The project identifier to use sprints from.
     * @param start_date The lower limit date in which sprints are contained.
     * @param end_date The upper limit date in which sprints are contained.
     * @return The sprint IDs of the sprint that are contained within the date
     * range, or an empty set if there are no sprints within the range.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public Set<Integer> find_sprints(int project_id, Timestamp start_date, Timestamp end_date) throws SQLException, PropertyVetoException {
        fillCache(project_id);
        JiraSprint[] sprints = dateCache.get(project_id);
        Set<Integer> rangeSprints = new TreeSet<>();
        
        int index = Bisect.bisectLeft(sprints, start_date);
        if (index != 0 && sprints[index-1].contains(start_date)) {
            index--;
        }
        while (index < sprints.length && sprints[index].compareTo(end_date) <= 0) {
            rangeSprints.add(sprints[index].getSprintId());
            index++;
        }
        
        return rangeSprints;
    }
    
    @Override
    public void close() throws SQLException {
        insertJiraStmt.execute();
        insertJiraStmt.close();
        
        updateJiraStmt.execute();
        updateJiraStmt.close();
        
        if (checkTfsStmt != null) {
            checkTfsStmt.close();
            checkTfsStmt = null;
        }
        
        insertTfsStmt.execute();
        insertTfsStmt.close();

        for (HashMap<Integer, JiraSprint> cache : keyCache.values()) {
            cache.clear();
        }
        keyCache.clear();
        dateCache.clear();
    }
    
    private void getCheckTfsStmt() throws SQLException, PropertyVetoException {
        if (checkTfsStmt == null) {
            Connection con = insertTfsStmt.getConnection();
            String sql = "SELECT sprint_id FROM gros.tfs_sprint WHERE project_id=? AND repo_id=? AND team_id=? AND name=?";
            checkTfsStmt = con.prepareStatement(sql);
        }
    }

    /**
     * Check whether a certain sprint exists in the the database and has the same
     * properties as the provided parameters.
     * @param project_id The project identifier.
     * @param name The human-readable name of the sprint as provided in TFS.
     * @param repo_id The internal identifier of the TFS repository.
     * @param team_id The internal identifier of the TFS team in the repository.
     * @return The sprint ID: null if the sprint name and project ID
     * combination is not found, or the sprint ID if a match is found.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public Integer check_tfs_sprint(int project_id, String name, Integer repo_id, Integer team_id) throws SQLException, PropertyVetoException {
        getCheckTfsStmt();
        
        checkTfsStmt.setInt(1, project_id);
        setInteger(checkTfsStmt, 2, repo_id);
        setInteger(checkTfsStmt, 3, team_id);
        checkTfsStmt.setString(4, name);
        
        Integer sprintId = null;
        try (ResultSet rs = checkTfsStmt.executeQuery()) {
            while (rs.next()) {
                sprintId = rs.getInt("sprint_id");
            }
        }
        return sprintId;
    }
    
    /**
     * Insert a new TFS sprint in the database.
     * @param project_id The project identifier.
     * @param name The human-readable name of the sprint as provided in JIRA.
     * @param start_date The date at which the sprint starts or is set to start.
     * @param end_date The date at which the sprint ends or is set to end.
     * @param repo_id The internal identifier of the TFS repository.
     * @param team_id The internal identifier of the TFS team in the repository.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void insert_tfs_sprint(int project_id, String name, Timestamp start_date, Timestamp end_date, Integer repo_id, Integer team_id) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertTfsStmt.getPreparedStatement();
        pstmt.setInt(1, project_id);
        pstmt.setString(2, name);
        setTimestamp(pstmt, 3, start_date);
        setTimestamp(pstmt, 4, end_date);
        setInteger(pstmt, 5, repo_id);
        setInteger(pstmt, 6, team_id);

        insertTfsStmt.batch();
    }
}
