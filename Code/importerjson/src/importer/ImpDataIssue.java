/*
 * Copyright 2017-2018 ICTU
 * 
 * Importer of gathered software development metrics for MonetDB
 */
package importer;

import dao.BatchedUpdateStatement;
import dao.ProjectDb;
import dao.SaltDb;
import java.beans.PropertyVetoException;
import util.BaseImport;
import java.io.FileReader;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import org.json.simple.JSONObject;
import util.BufferedJSONReader;

/**
 * Import for JIRA issues.
 * @author Enrique
 */
public class ImpDataIssue extends BaseImport {
    private static final String[] FIELDS = {
        "issue_id", "changelog_id", "key", "title", "type", "priority",
        "resolution", "fixversion", "bugfix", "watchers", "created",
        "updated", "description", "duedate", "project_id", "status",
        "reporter", "assignee", "attachments", "additional_information",
        "review_comments", "story_points", "resolution_date", "sprint_id",
        "updated_by", "rank_change", "epic", "impediment", "ready_status",
        "ready_status_reason", "approved", "approved_by_po", "labels",
        "version", "expected_ltcs", "expected_phtcs", "test_given",
        "test_when", "test_then", "test_execution", "test_execution_time",
        "environment", "external_project", "encryption"
    };

    private static final BigDecimal MAX_POINTS = BigDecimal.valueOf(999.0);
    
    private static String getInsertSql() {
        String[] fields = new String[FIELDS.length];
        Arrays.fill(fields, "?");
        return "insert into gros.issue values (" + String.join(",", fields) + ");";
    }

    private static String getUpdateSql() {
        StringBuilder updateSql = new StringBuilder("update gros.issue set ");
        for (int i = 2; i < FIELDS.length; i++) {
            if (i > 2) {
                updateSql.append(", ");
            }
            updateSql.append(FIELDS[i]).append("=?");
        }
        updateSql.append(" where issue_id=? and changelog_id=?;");
        return updateSql.toString();
    }

    private class BatchedIssueStatement extends BatchedUpdateStatement {
        private final int projectID = getProjectID();
        private final String projectName = getProjectName();
        private final ProjectDb projectDb = new ProjectDb();
        
        public BatchedIssueStatement(String insertSql, String updateSql) {
            super("gros.issue", insertSql, updateSql, new String[]{"issue_id", "changelog_id"});
        }

        private String getField(JSONObject jsonObject, String field) {
            String value = (String) jsonObject.get(field);
            // Convert zeroes to null values
            if ("0".equals(value)) {
                return null;
            }
            return value;
        }

        private void makeBatch(Object[] values, Object data, PreparedStatement pstmt, boolean is_update) throws SQLException, PropertyVetoException {
            int issue_id = (int)values[0];
            int changelog_id = (int)values[1];

            // Keep fields for which "0" could be a valid value as is.
            // E.g., integral amounts, user input text, flags (but not -1/1 booleans).
            // User names cannot be "0" naturally for backward compatibility.
            // Fields that do not have "0" as valid value are parsed using getField.
            // Fields that can be null, including the ones parsed with
            // getField, are passed through one of the set* methods.
            JSONObject jsonObject = (JSONObject) data;

            // Fill the prepared statement with the new field values.
            int index = 0;
            if (!is_update) {
                pstmt.setInt(++index, issue_id);
                pstmt.setInt(++index, changelog_id);
            }
            pstmt.setString(++index, (String) jsonObject.get("key"));
            pstmt.setString(++index, (String) jsonObject.get("title"));
            setInteger(pstmt, ++index, (String) jsonObject.get("issuetype"));
            setInteger(pstmt, ++index, getField(jsonObject, "priority"));
            setInteger(pstmt, ++index, getField(jsonObject, "resolution"));
            setInteger(pstmt, ++index, getField(jsonObject, "fixVersions"));
            setBoolean(pstmt, ++index, getField(jsonObject, "bugfix"));
            pstmt.setInt(++index, Integer.parseInt((String) jsonObject.get("watchers")));

            setTimestamp(pstmt, ++index, getField(jsonObject, "created"));
            setTimestamp(pstmt, ++index, getField(jsonObject, "updated"));

            setString(pstmt, ++index, (String) jsonObject.get("description"));

            setDate(pstmt, ++index, getField(jsonObject, "duedate"));

            pstmt.setInt(++index, makeProjectID((String) jsonObject.get("project")));
            setInteger(pstmt, ++index, getField(jsonObject, "status"));
            setString(pstmt, ++index, getField(jsonObject, "reporter"));
            setString(pstmt, ++index, getField(jsonObject, "assignee"));
            pstmt.setInt(++index, Integer.parseInt((String) jsonObject.get("attachment")));
            setString(pstmt, ++index, (String) jsonObject.get("additional_information"));
            setString(pstmt, ++index, (String) jsonObject.get("review_comments"));

            setDouble(pstmt, ++index, getField(jsonObject, "storypoint"), MAX_POINTS);

            setTimestamp(pstmt, ++index, getField(jsonObject, "resolution_date"));

            setInteger(pstmt, ++index, getField(jsonObject, "sprint"));

            setString(pstmt, ++index, (String) jsonObject.get("updated_by"));
            setBoolean(pstmt, ++index, getField(jsonObject, "rank_change"));

            setString(pstmt, ++index, getField(jsonObject, "epic"));

            // Ready status
            pstmt.setBoolean(++index, "1".equals((String) jsonObject.get("flagged")));
            setInteger(pstmt, ++index, getField(jsonObject, "ready_status"));
            setString(pstmt, ++index, (String) jsonObject.get("ready_status_reason"));
            setBoolean(pstmt, ++index, (String) jsonObject.get("approved"));
            setBoolean(pstmt, ++index, (String) jsonObject.get("approved_by_po"));

            pstmt.setInt(++index, Integer.parseInt((String) jsonObject.get("labels")));
            setInteger(pstmt, ++index, getField(jsonObject, "versions"));

            // Test cases
            setInteger(pstmt, ++index, (String) jsonObject.get("expected_ltcs"));
            setInteger(pstmt, ++index, (String) jsonObject.get("expected_phtcs"));
            setString(pstmt, ++index, getField(jsonObject, "test_given"));
            setString(pstmt, ++index, getField(jsonObject, "test_when"));
            setString(pstmt, ++index, getField(jsonObject, "test_then"));
            setInteger(pstmt, ++index, getField(jsonObject, "test_execution"));
            setInteger(pstmt, ++index, (String) jsonObject.get("test_execution_time"));

            setString(pstmt, ++index, (String) jsonObject.get("environment"), 100);
            setString(pstmt, ++index, (String) jsonObject.get("external_project"));

            // Encryption
            pstmt.setInt(++index, SaltDb.Encryption.NONE);

            if (is_update) {
                pstmt.setInt(++index, issue_id);
                pstmt.setInt(++index, changelog_id);
            }
        }

        private void setBoolean(PreparedStatement pstmt, int index, String value) throws SQLException {
            // For booleans, "-1" means false, "1" means true, and null or "0" means missing.
            if (value == null || value.equals("0")) {
                pstmt.setNull(index, java.sql.Types.BOOLEAN);
            }
            else {
                pstmt.setBoolean(index, value.equals("1"));
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
            this.projectDb.close();
        }

        private int makeProjectID(String project) throws PropertyVetoException, SQLException {
            if (project == null) {
                return projectID;
            }
            else {
                int project_id = projectDb.check_project(project);
                if (project_id == 0) {
                    // Insert a dummy project such that we can link to it.
                    // Either this project is imported later on, or
                    // the project will never get any other data and is thus
                    // free from sprints and repositories. Mark it as a
                    // subproject of this one, which is overwritten if it
                    // is an actual project that we import, and helps hiding
                    // it from most purposes.
                    projectDb.insert_project(project, projectName, null, null, null, null, null);
                    project_id = projectDb.check_project(project);
                }
                return project_id;
            }
        }
    }
    
    @Override
    public void parser() {        
        try (
            FileReader fr = new FileReader(getMainImportPath());
            BufferedJSONReader br = new BufferedJSONReader(fr);
            BatchedUpdateStatement cstmt = new BatchedIssueStatement(getInsertSql(), getUpdateSql());
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
        return "JIRA issues";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data.json"};
    }
}
    
