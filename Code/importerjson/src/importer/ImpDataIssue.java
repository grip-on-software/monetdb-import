/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.BatchedStatement;
import util.BaseImport;
import java.io.FileReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Enrique
 */
public class ImpDataIssue extends BaseImport{
    
    @Override
    public void parser() {
        PreparedStatement existsStmt = null;
        ResultSet rs = null;
        JSONParser parser = new JSONParser();
        int projectId = getProjectID();
        String[] fields = new String[42];
        Arrays.fill(fields, "?");
        String sql = "insert into gros.issue values (" + String.join(",", fields) + ");";
        
        try (
            FileReader fr = new FileReader(getPath()+getProjectName()+"/data.json");
            BatchedStatement bstmt = new BatchedStatement(sql);
        ) {
            PreparedStatement pstmt = bstmt.getPreparedStatement();
            
            Connection con = bstmt.getConnection();
            
            sql = "SELECT * FROM gros.issue WHERE issue_id = ? AND changelog_id = ?";
            existsStmt = con.prepareStatement(sql);
                
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String additional_information = (String) jsonObject.get("additional_information");
                String assignee = (String) jsonObject.get("assignee");
                String title = (String) jsonObject.get("title");
                String fixVersions = (String) jsonObject.get("fixVersions");
                String priority = (String) jsonObject.get("priority");
                String attachment = (String) jsonObject.get("attachment");
                String type = (String) jsonObject.get("issuetype");
                String duedate = (String) jsonObject.get("duedate");
                String status = (String) jsonObject.get("status");
                String updated = (String) jsonObject.get("updated");
                String updated_by = (String) jsonObject.get("updated_by");
                String description = (String) jsonObject.get("description");
                String reporter = (String) jsonObject.get("reporter");
                String key = (String) jsonObject.get("key");
                String resolution_date = (String) jsonObject.get("resolution_date");
                String storypoint = (String) jsonObject.get("storypoint");
                String watchers = (String) jsonObject.get("watchers");
                String created = (String) jsonObject.get("created");  
                String bugfix = (String) jsonObject.get("bugfix");
                String review_comments = (String) jsonObject.get("review_comments");
                String issue_id = (String) jsonObject.get("issue_id");
                String resolution = (String) jsonObject.get("resolution");
                String sprint = (String) jsonObject.get("sprint");
                String changelog_id = (String) jsonObject.get("changelog_id");
                String rank_change = (String) jsonObject.get("rank_change");
                String epic = (String) jsonObject.get("epic");
                String flagged = (String) jsonObject.get("flagged");
                String ready_status = (String) jsonObject.get("ready_status");
                String ready_status_reason = (String) jsonObject.get("ready_status_reason");
                String approved = (String) jsonObject.get("approved");
                String approved_by_po = (String) jsonObject.get("approved_by_po");
                String labels = (String) jsonObject.get("labels");
                String affectedVersion = (String) jsonObject.get("versions");
                String expected_ltcs = (String) jsonObject.get("expected_ltcs");
                String expected_phtcs = (String) jsonObject.get("expected_phtcs");
                String test_given = (String) jsonObject.get("test_given");
                String test_when = (String) jsonObject.get("test_when");
                String test_then = (String) jsonObject.get("test_then");
                String test_execution = (String) jsonObject.get("test_execution");
                String test_execution_time = (String) jsonObject.get("test_execution_time");
                
                existsStmt.setInt(1, Integer.parseInt(issue_id));
                existsStmt.setInt(2, Integer.parseInt(changelog_id));
                rs = existsStmt.executeQuery();
                
                // Check if the issue/changelog id pair does not already exist
                if(!rs.next()) {
                    // Convert legacy format (null, None) and empty fields ("0") from the JSON fields to correct values.
                    if ((sprint.trim()).equals("null")){
                        sprint = "0";
                    }

                    if ((resolution.trim()).equals("None")){
                        resolution = "0";
                    }
                    if ((assignee.trim()).equals("0") || (assignee.trim()).equals("None")){
                        assignee = "None";
                    }
                    if ((reporter.trim()).equals("0") || (reporter.trim()).equals("None")){
                        reporter = "None";
                    }
                    if ((assignee.trim()).equals("0") || (assignee.trim()).equals("None")){
                        assignee = "None";
                    }
                    if ((created.trim()).equals("0") || (created.trim()).equals("None") ){
                        created = null;
                    }
                    if ((updated.trim()).equals("0") || (updated.trim()).equals("None")){
                        updated = null;
                    }
                    if ((duedate.trim()).equals("0") || (duedate.trim()).equals("None")){
                        duedate = null;
                    }
                    if ((resolution_date.trim()).equals("0") || (resolution_date.trim()).equals("None")){
                        resolution_date = null;
                    }
                    if ((storypoint.trim()).equals("0") || (storypoint.trim()).equals("None")){
                        storypoint = null;
                    }
                    if ((bugfix.trim()).equals("None")){
                        bugfix = "0";
                    }
                    if (epic.equals("0")) {
                        epic = null;
                    }

                    // Fill the prepared statement with the new field values.
                    pstmt.setInt(1, Integer.parseInt(issue_id));
                    pstmt.setInt(2, Integer.parseInt(changelog_id));
                    pstmt.setString(3, key);
                    pstmt.setString(4, title);
                    pstmt.setInt(5, Integer.parseInt(type));
                    pstmt.setInt(6, Integer.parseInt(priority));
                    pstmt.setInt(7, Integer.parseInt(resolution));
                    pstmt.setInt(8, Integer.parseInt(fixVersions));
                    setBoolean(pstmt, 9, bugfix);
                    pstmt.setInt(10, Integer.parseInt(watchers));

                    setTimestamp(pstmt, 11, created);
                    setTimestamp(pstmt, 12, updated);

                    pstmt.setString(13, description);

                    setDate(pstmt, 14, duedate);

                    pstmt.setInt(15, projectId);
                    pstmt.setInt(16, Integer.parseInt(status));
                    pstmt.setString(17, "");
                    pstmt.setString(18, reporter);
                    pstmt.setString(19, assignee);
                    pstmt.setInt(20, Integer.parseInt(attachment));
                    pstmt.setString(21, additional_information);
                    pstmt.setString(22, review_comments);
                    
                    if (storypoint != null) {
                        BigDecimal points = BigDecimal.valueOf(Double.parseDouble(storypoint));
                        pstmt.setBigDecimal(23, points);
                    }
                    else {
                        pstmt.setNull(23, java.sql.Types.DECIMAL);
                    }

                    setTimestamp(pstmt, 24, resolution_date);

                    pstmt.setInt(25, Integer.parseInt(sprint));                

                    if (updated_by.isEmpty()) {
                        updated_by = reporter;
                    }

                    pstmt.setString(26, updated_by);
                    setBoolean(pstmt, 27, rank_change);
                    
                    if (epic != null) {
                        pstmt.setString(28, epic);
                    }
                    else {
                        pstmt.setNull(28, java.sql.Types.VARCHAR);
                    }
                    
                    pstmt.setBoolean(29, flagged.equals("1"));
                    pstmt.setInt(30, Integer.parseInt(ready_status));
                    pstmt.setString(31, ready_status_reason);
                    setBoolean(pstmt, 32, approved);
                    setBoolean(pstmt, 33, approved_by_po);
                    
                    pstmt.setInt(34, Integer.parseInt(labels));
                    pstmt.setInt(35, Integer.parseInt(affectedVersion));
                    
                    pstmt.setInt(36, Integer.parseInt(expected_ltcs));
                    pstmt.setInt(37, Integer.parseInt(expected_phtcs));
                    pstmt.setString(38, test_given);
                    pstmt.setString(39, test_when);
                    pstmt.setString(40, test_then);
                    pstmt.setInt(41, Integer.parseInt(test_execution));
                    pstmt.setInt(42, Integer.parseInt(test_execution_time));

                    bstmt.batch();
                }
            }
            
            bstmt.execute();
        }
        catch (Exception ex) {
            logException(ex);
        }
        finally {
            if (existsStmt != null) try { existsStmt.close(); } catch (SQLException ex) {logException(ex);}
            if (rs != null) try { rs.close(); } catch (SQLException ex) {logException(ex);}
        }
        
    }
    
    private void setBoolean(PreparedStatement pstmt, int index, String value) throws SQLException {
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
}
    
