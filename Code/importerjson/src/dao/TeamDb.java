/**
 * TFS team and team member tables.
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
import java.util.HashMap;
import java.util.Objects;
import util.BaseDb;

/**
 * Database access magement for TFS team properties.
 * @author Leon Helwerda
 */
public class TeamDb extends BaseDb implements AutoCloseable {
    private BatchedStatement insertTfsTeamStmt = null;
    private PreparedStatement checkTfsTeamStmt = null;
    private BatchedStatement updateTfsTeamStmt = null;
    
    private BatchedStatement insertTfsMemberStmt = null;
    private PreparedStatement checkTfsMemberStmt = null;

    private final HashMap<Integer, HashMap<String, Team>> nameCache = new HashMap<>();

    public static class Team {
        private final int team_id;
        private final String team_name;
        private final int project_id;
        private final int repo_id;
        private final String description;
        
        public Team(int team_id, String team_name, int project_id, int repo_id, String description) {
            this.team_id = team_id;
            this.team_name = team_name;
            this.project_id = project_id;
            this.repo_id = repo_id;
            this.description = description;
        }

        public int getTeamId() {
            return team_id;
        }
        
        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (other instanceof Team) {
                Team otherTeam = (Team)other;
                return (team_id == otherTeam.team_id &&
                        team_name.equals(otherTeam.team_name) &&
                        project_id == otherTeam.project_id &&
                        repo_id == otherTeam.repo_id &&
                        (description == null ? otherTeam.description == null : description.equals(otherTeam.description)));
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 61 * hash + this.team_id;
            hash = 61 * hash + Objects.hashCode(this.team_name);
            hash = 61 * hash + this.project_id;
            hash = 61 * hash + this.repo_id;
            hash = 61 * hash + Objects.hashCode(this.description);
            return hash;
        }
    }

    public TeamDb() {
        String sql = "insert into gros.tfs_team (project_id,repo_id,name,description) values (?,?,?,?);";
        insertTfsTeamStmt = new BatchedStatement(sql);
        
        sql = "update gros.tfs_team set repo_id = ?, description = ? where team_id = ?";
        updateTfsTeamStmt = new BatchedStatement(sql);
        
        sql = "insert into gros.tfs_team_member (team_id,repo_id,alias_id,name,display_name,encryption) values (?,?,?,?,?,?);";
        insertTfsMemberStmt = new BatchedStatement(sql);
    }
    
    @Override
    public void close() throws Exception {
        // All insert statements are executed immediately, so no need to do so now
        insertTfsTeamStmt.close();
        
        updateTfsTeamStmt.execute();
        updateTfsTeamStmt.close();
        
        if (checkTfsTeamStmt != null) {
            checkTfsTeamStmt.close();
            checkTfsTeamStmt = null;
        }
        
        insertTfsMemberStmt.execute();
        insertTfsMemberStmt.close();
        
        if (checkTfsMemberStmt != null) {
            checkTfsMemberStmt.close();
            checkTfsMemberStmt = null;
        }
    }
    
    private void getCheckTfsTeamStmt() throws SQLException, PropertyVetoException {
        if (checkTfsTeamStmt == null) {
            Connection con = insertTfsTeamStmt.getConnection();
            checkTfsTeamStmt = con.prepareStatement("SELECT team_id, repo_id, description FROM gros.tfs_team WHERE project_id = ? AND name = ?");
        }
    }
    
    private void insertNameCache(Integer project_id, String name, Team cache) {
        if (!nameCache.containsKey(project_id)) {
            nameCache.put(project_id, new HashMap<String, Team>());
        }
        nameCache.get(project_id).put(name, cache);
    }

    public Team check_tfs_team(String team_name, int project_id) throws SQLException, PropertyVetoException {
        if (nameCache.containsKey(project_id)) {
            Team cache = nameCache.get(project_id).get(team_name);
            if (cache != null) {
                return cache;
            }
        }
        
        getCheckTfsTeamStmt();
        
        checkTfsTeamStmt.setInt(1, project_id);
        checkTfsTeamStmt.setString(2, team_name);
        
        Team team = null;
        try (ResultSet rs = checkTfsTeamStmt.executeQuery()) {
            if (rs.next()) {
                team = new Team(rs.getInt("team_id"), team_name, project_id, rs.getInt("repo_id"), rs.getString("description"));
            }
        }
        insertNameCache(project_id, team_name, team);
        return team;
    }

    public void insert_tfs_team(String team_name, int project_id, int repo_id, String description) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertTfsTeamStmt.getPreparedStatement();
        
        pstmt.setInt(1, project_id);
        pstmt.setInt(2, repo_id);
        pstmt.setString(3, team_name);
        setString(pstmt, 4, description);
        
        // Execute immediately because we need to have the team_id available
        // for check_tfs_team
        pstmt.execute();
    }
    
    public void update_tfs_team(Team team) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = updateTfsTeamStmt.getPreparedStatement();
        
        pstmt.setInt(1, team.repo_id);
        setString(pstmt, 2, team.description);
        pstmt.setInt(3, team.getTeamId());
        
        insertNameCache(team.project_id, team.team_name, team);
        updateTfsTeamStmt.batch();
    }
    

    private void getCheckTfsMemberStmt() throws SQLException, PropertyVetoException {
        if (checkTfsMemberStmt == null) {
            Connection con = insertTfsMemberStmt.getConnection();
            checkTfsMemberStmt = con.prepareStatement("SELECT name FROM gros.tfs_team_member WHERE team_id = ? AND name = ?");
        }
    }
    
    public boolean check_tfs_team_member(Team team, String name) throws SQLException, PropertyVetoException {
        getCheckTfsMemberStmt();
        
        checkTfsMemberStmt.setInt(1, team.getTeamId());
        checkTfsMemberStmt.setString(2, name);
        
        try (ResultSet rs = checkTfsMemberStmt.executeQuery()) {
            if (rs.next()) {
                return true;
            }
        }
        return false;
    }

    public void insert_tfs_team_member(Team team, Integer alias_id, String name, String display_name, int encryption) throws SQLException, PropertyVetoException {
        PreparedStatement pstmt = insertTfsMemberStmt.getPreparedStatement();
        
        pstmt.setInt(1, team.getTeamId());
        pstmt.setInt(2, team.repo_id);
        setInteger(pstmt, 3, alias_id);
        setString(pstmt, 4, name);
        setString(pstmt, 5, display_name);
        pstmt.setInt(6, encryption);
        
        insertTfsMemberStmt.batch();
    }
}
