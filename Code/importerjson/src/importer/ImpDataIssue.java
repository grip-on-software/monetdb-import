/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.BatchedCheckStatement;
import dao.ProjectDb;
import dao.SaltDb;
import java.beans.PropertyVetoException;
import util.BaseImport;
import java.io.FileReader;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
    private static final int NUMBER_OF_FIELDS = 44;
    
    @Override
    public void parser() {
        PreparedStatement existsStmt = null;
        ResultSet rs = null;
        String[] fields = new String[NUMBER_OF_FIELDS];
        Arrays.fill(fields, "?");
        String sql = "insert into gros.issue values (" + String.join(",", fields) + ");";
        
        try (
            FileReader fr = new FileReader(getMainImportPath());
            BufferedJSONReader br = new BufferedJSONReader(fr);
            BatchedCheckStatement cstmt = new BatchedCheckStatement("gros.issue", sql,
                    new String[]{"issue_id", "changelog_id"}
            ) {
                private final BigDecimal max_points = BigDecimal.valueOf(999.0);
                private final int projectID = getProjectID();
                private final String projectName = getProjectName();
                private final ProjectDb projectDb = new ProjectDb();
                
                private String getField(JSONObject jsonObject, String field) {
                    String value = (String) jsonObject.get(field);
                    // Convert zeroes to null values
                    if ("0".equals(value)) {
                        return null;
                    }
                    return value;
                }

                @Override
                protected void addToBatch(Object[] values, Object data, PreparedStatement pstmt) throws SQLException, PropertyVetoException {
                    int issue_id = (int)values[0];
                    int changelog_id = (int)values[1];
                    
                    // Keep fields for which "0" could be a valid value as is.
                    // E.g., integral amounts, user input text, flags (but not -1/1 booleans).
                    // User names cannot be "0" naturally for backward compatibility.
                    // Fields that do not have "0" as valid value are parsed using getField.
                    // Fields that can be null, including the ones parsed with
                    // getField, are passed through one of the set* methods.
                    JSONObject jsonObject = (JSONObject) data;
                    String additional_information = (String) jsonObject.get("additional_information");
                    String assignee = getField(jsonObject, "assignee");
                    String title = (String) jsonObject.get("title");
                    String fixVersions = getField(jsonObject, "fixVersions");
                    String priority = getField(jsonObject, "priority");
                    String attachment = (String) jsonObject.get("attachment");
                    String type = (String) jsonObject.get("issuetype");
                    String duedate = getField(jsonObject, "duedate");
                    String status = getField(jsonObject, "status");
                    String updated = getField(jsonObject, "updated");
                    String updated_by = (String) jsonObject.get("updated_by");
                    String description = (String) jsonObject.get("description");
                    String reporter = getField(jsonObject, "reporter");
                    String key = (String) jsonObject.get("key");
                    String project = (String) jsonObject.get("project");
                    String resolution_date = getField(jsonObject, "resolution_date");
                    String storypoint = getField(jsonObject, "storypoint");
                    String watchers = (String) jsonObject.get("watchers");
                    String created = getField(jsonObject, "created");
                    String bugfix = getField(jsonObject, "bugfix");
                    String review_comments = (String) jsonObject.get("review_comments");
                    String resolution = getField(jsonObject, "resolution");
                    String sprint = getField(jsonObject, "sprint");
                    String rank_change = getField(jsonObject, "rank_change");
                    String epic = getField(jsonObject, "epic");
                    String flagged = (String) jsonObject.get("flagged");
                    String ready_status = getField(jsonObject, "ready_status");
                    String ready_status_reason = (String) jsonObject.get("ready_status_reason");
                    String approved = (String) jsonObject.get("approved");
                    String approved_by_po = (String) jsonObject.get("approved_by_po");
                    String labels = (String) jsonObject.get("labels");
                    String affected_version = getField(jsonObject, "versions");
                    String expected_ltcs = (String) jsonObject.get("expected_ltcs");
                    String expected_phtcs = (String) jsonObject.get("expected_phtcs");
                    String test_given = getField(jsonObject, "test_given");
                    String test_when = getField(jsonObject, "test_when");
                    String test_then = getField(jsonObject, "test_then");
                    String test_execution = getField(jsonObject, "test_execution");
                    String test_execution_time = (String) jsonObject.get("test_execution_time");
                    String environment = (String) jsonObject.get("environment");
                    String external_project = (String) jsonObject.get("external_project");
                    
                    // Fill the prepared statement with the new field values.
                    pstmt.setInt(1, issue_id);
                    pstmt.setInt(2, changelog_id);
                    pstmt.setString(3, key);
                    pstmt.setString(4, title);
                    setInteger(pstmt, 5, type);
                    setInteger(pstmt, 6, priority);
                    setInteger(pstmt, 7, resolution);
                    setInteger(pstmt, 8, fixVersions);
                    setBoolean(pstmt, 9, bugfix);
                    pstmt.setInt(10, Integer.parseInt(watchers));

                    setTimestamp(pstmt, 11, created);
                    setTimestamp(pstmt, 12, updated);

                    setString(pstmt, 13, description);

                    setDate(pstmt, 14, duedate);

                    if (project == null) {
                        pstmt.setInt(15, projectID);
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
                        pstmt.setInt(15, project_id);
                    }
                    setInteger(pstmt, 16, status);
                    setString(pstmt, 17, reporter);
                    setString(pstmt, 18, assignee);
                    pstmt.setInt(19, Integer.parseInt(attachment));
                    setString(pstmt, 20, additional_information);
                    setString(pstmt, 21, review_comments);
                    
                    if (storypoint != null) {
                        BigDecimal points = BigDecimal.valueOf(Double.parseDouble(storypoint));
                        pstmt.setBigDecimal(22, points.min(max_points));
                    }
                    else {
                        pstmt.setNull(22, java.sql.Types.NUMERIC);
                    }

                    setTimestamp(pstmt, 23, resolution_date);

                    setInteger(pstmt, 24, sprint);

                    setString(pstmt, 25, updated_by);
                    setBoolean(pstmt, 26, rank_change);
                    
                    setString(pstmt, 27, epic);
                    
                    // Ready status
                    pstmt.setBoolean(28, flagged.equals("1"));
                    setInteger(pstmt, 29, ready_status);
                    setString(pstmt, 30, ready_status_reason);
                    setBoolean(pstmt, 31, approved);
                    setBoolean(pstmt, 32, approved_by_po);
                    
                    pstmt.setInt(33, Integer.parseInt(labels));
                    setInteger(pstmt, 34, affected_version);
                    
                    // Test cases
                    pstmt.setInt(35, Integer.parseInt(expected_ltcs));
                    pstmt.setInt(36, Integer.parseInt(expected_phtcs));
                    setString(pstmt, 37, test_given);
                    setString(pstmt, 38, test_when);
                    setString(pstmt, 39, test_then);
                    setInteger(pstmt, 40, test_execution);
                    pstmt.setInt(41, Integer.parseInt(test_execution_time));
                    
                    setString(pstmt, 42, environment, 100);
                    setString(pstmt, 43, external_project);
                    
                    // Encryption
                    pstmt.setInt(44, SaltDb.Encryption.NONE);

                    insertStmt.batch();
                }
            };
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
        finally {
            if (existsStmt != null) try { existsStmt.close(); } catch (SQLException ex) {logException(ex);}
            if (rs != null) try { rs.close(); } catch (SQLException ex) {logException(ex);}
        }
        
    }
    
    private void setInteger(PreparedStatement pstmt, int index, String value) throws SQLException {
        if (value != null) {
            int number = Integer.parseInt(value);
            pstmt.setInt(index, number);
        } else{
            pstmt.setNull(index, java.sql.Types.INTEGER);
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

    private void setTimestamp(PreparedStatement pstmt, int index, String value) throws SQLException {
        if (value != null){
            Timestamp date = Timestamp.valueOf(value);
            pstmt.setTimestamp(index, date);
        } else{
            pstmt.setNull(index, java.sql.Types.TIMESTAMP);
        }
    }

    private void setDate(PreparedStatement pstmt, int index, String value) throws SQLException {
        if (value != null){
            Date date = Date.valueOf(value);
            pstmt.setDate(index, date);
        } else{
            pstmt.setNull(index, java.sql.Types.DATE);
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
    
