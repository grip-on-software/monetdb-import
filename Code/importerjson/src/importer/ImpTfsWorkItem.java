/*
 * Copyright 2017-2018 ICTU
 * 
 * Importer of gathered software development metrics for MonetDB
 */
package importer;

import dao.BatchedUpdateStatement;
import dao.SprintDb;
import dao.TeamDb;
import java.beans.PropertyVetoException;
import java.io.FileReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import org.json.simple.JSONObject;
import util.BaseImport;
import util.BufferedJSONReader;

/**
 * Importer for TFS work item issue changelog versions.
 * @author Leon Helwerda
 */
public class ImpTfsWorkItem extends BaseImport {
    private static final String[] FIELDS = {
        "issue_id", "changelog_id", "title", "type", "priority", "created",
        "updated", "description", "duedate", "project_id", "status", "reporter",
        "assignee", "attachments", "additional_information", "sprint_id",
        "team_id", "updated_by", "labels"
    };

    private static String getInsertSql() {
        String[] fields = new String[FIELDS.length];
        Arrays.fill(fields, "?");
        return "insert into gros.tfs_work_item values (" + String.join(",", fields) + ");";
    }

    private static String getUpdateSql() {
        StringBuilder updateSql = new StringBuilder("update gros.tfs_work_item set ");
        for (int i = 2; i < FIELDS.length; i++) {
            if (i > 2) {
                updateSql.append(", ");
            }
            updateSql.append(FIELDS[i]).append("=?");
        }
        updateSql.append(" where issue_id=? and changelog_id=?;");
        return updateSql.toString();
    }

    private class BatchedWorkItemStatement extends BatchedUpdateStatement {
        private final int projectID = getProjectID();
        private final SprintDb sprintDb = new SprintDb();
        private final TeamDb teamDb = new TeamDb();
        private final String projectName = getProjectName();
        
        public BatchedWorkItemStatement(String insertSql, String updateSql) {
            super("gros.tfs_work_item", insertSql, updateSql, new String[]{"issue_id", "changelog_id"});
        }

        private void makeBatch(Object[] values, Object data, PreparedStatement pstmt, boolean is_update) throws SQLException, PropertyVetoException {
            int issue_id = (int)values[0];
            int changelog_id = (int)values[1];
            
            JSONObject jsonObject = (JSONObject) data;
            
            int index = 0;
            if (!is_update) {
                pstmt.setInt(++index, issue_id);
                pstmt.setInt(++index, changelog_id);
            }
            
            pstmt.setString(++index, (String) jsonObject.get("title"));
            setString(pstmt, ++index, (String) jsonObject.get("issuetype"));
            setInteger(pstmt, ++index, (String) jsonObject.get("priority"));
            setTimestamp(pstmt, ++index, (String) jsonObject.get("created_date"));
            setTimestamp(pstmt, ++index, (String) jsonObject.get("updated_date"));
            setString(pstmt, ++index, (String) jsonObject.get("description"));
            setDate(pstmt, ++index, (String) jsonObject.get("duedate"));
            pstmt.setInt(++index, projectID);
            setString(pstmt, ++index, (String) jsonObject.get("status"));
            setString(pstmt, ++index, (String) jsonObject.get("reporter"), 100);
            setString(pstmt, ++index, (String) jsonObject.get("assignee"), 100);
            setInteger(pstmt, ++index, (String) jsonObject.get("attachments"));
            setString(pstmt, ++index, (String) jsonObject.get("additional_information"));
            
            String sprint_name = (String) jsonObject.get("sprint_name");
            Integer sprint_id = null;
            if (sprint_name != null) {
                sprint_id = sprintDb.check_tfs_sprint(projectID, sprint_name);
            }
            setInteger(pstmt, ++index, sprint_id);
            
            String team_name = (String) jsonObject.get("team_name");
            Integer team_id = null;
            if (team_name != null) {
                TeamDb.Team team = teamDb.check_tfs_team(team_name, projectID);
                if (team != null) {
                    team_id = team.getTeamId();
                }
            }
            setInteger(pstmt, ++index, team_id);
            
            setString(pstmt, ++index, (String) jsonObject.get("updated_by"), 100);
            setInteger(pstmt, ++index, (String) jsonObject.get("labels"));

            if (is_update) {
                pstmt.setInt(++index, issue_id);
                pstmt.setInt(++index, changelog_id);
            }
        }
        
        @Override
        protected void addToBatch(Object[] values, Object data, PreparedStatement pstmt) throws SQLException, PropertyVetoException {
            makeBatch(values, data, pstmt, false);
            insertStmt.batch();
        }

        @Override
        protected void addToUpdateBatch(Object[] values, Object data, PreparedStatement pstmt) throws SQLException, PropertyVetoException {
            makeBatch(values, data, pstmt, true);
            updateStmt.batch();
        }
        
        @Override
        public void close() throws SQLException {
            super.close();
            this.sprintDb.close();
        }
    }

    @Override
    public void parser() {
        try (
            FileReader fr = new FileReader(getMainImportPath());
            BufferedJSONReader br = new BufferedJSONReader(fr);
            BatchedUpdateStatement cstmt = new BatchedWorkItemStatement(getInsertSql(), getUpdateSql());
        ) {
            Object o;
            while ((o = br.readObject()) != null) {
                JSONObject jsonObject = (JSONObject) o;
                
                String issue_id = (String) jsonObject.get("issue_id");
                String changelog_id = (String) jsonObject.get("changelog_id");
                
                Object[] values = new Object[]{
                    Integer.parseInt(issue_id), Integer.parseInt(changelog_id)
                };
                
                cstmt.batch(values, o);
            }
            
            cstmt.execute();
        }
        catch (Exception ex) {
            logException(ex);
        }
    }

    @Override
    public String getImportName() {
        return "TFS work items";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_tfs_work_item.json"};
    }
    
}
