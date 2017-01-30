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
    public void parser(){

        BatchedStatement bstmt = null;
        PreparedStatement pstmt = null;
        PreparedStatement existsStmt = null;
        ResultSet rs = null;
        JSONParser parser = new JSONParser();
        String new_description = "";
        int projectId = getProjectID();
        
        try {
            String[] fields = new String[31];
            Arrays.fill(fields, "?");
            String sql = "insert into gros.issue values (" + String.join(",", fields) + ");";
            bstmt = new BatchedStatement(sql);
            pstmt = bstmt.getPreparedStatement();
            
            Connection con = bstmt.getConnection();
            
            sql = "SELECT * FROM gros.issue WHERE issue_id = ? AND changelog_id = ?";
            existsStmt = con.prepareStatement(sql);
                
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+getProjectName()+"/data.json"));
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String additional_information = (String) jsonObject.get("additional_information");
                String assignee = (String) jsonObject.get("assignee");
                String title = (String) jsonObject.get("title");
                String fixVersions = (String) jsonObject.get("fixVersions");
                String priority = (String) jsonObject.get("priority");
                String attachment = (String) jsonObject.get("attachment");
                String type = (String) jsonObject.get("type");
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
                String labels = (String) jsonObject.get("labels");
                
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
                    if (rank_change.equals("0")) {
                        rank_change = null;
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
                    pstmt.setInt(9, Integer.parseInt(bugfix));
                    pstmt.setInt(10, Integer.parseInt(watchers));

                    Timestamp ts_created;              
                    if (created != null){
                        ts_created = Timestamp.valueOf(created); 
                        pstmt.setTimestamp(11, ts_created);
                    } else{
                        pstmt.setNull(11, java.sql.Types.TIMESTAMP);
                    }

                    Timestamp ts_updated;              
                    if (updated != null){
                        ts_updated = Timestamp.valueOf(updated); 
                        pstmt.setTimestamp(12, ts_updated);
                    } else{
                        pstmt.setNull(12, java.sql.Types.TIMESTAMP);
                    }

                    new_description = description.replace("'","\\'");
                    pstmt.setString(13, new_description);

                    Timestamp ts_duedate;              
                    if (duedate != null){
                        ts_duedate = Timestamp.valueOf(duedate); 
                        pstmt.setTimestamp(14, ts_duedate);
                    } else{
                        pstmt.setNull(14, java.sql.Types.TIMESTAMP);
                    }

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

                    Timestamp ts_resolution_date;              
                    if (resolution_date != null){

                        ts_resolution_date = Timestamp.valueOf(resolution_date); 

                        pstmt.setTimestamp(24, ts_resolution_date);
                    } else{
                        //ts_resolution_date = null;
                        pstmt.setNull(24, java.sql.Types.TIMESTAMP);
                    }

                    pstmt.setInt(25, Integer.parseInt(sprint));                

                    if (updated_by.isEmpty()) {
                        updated_by = reporter;
                    }

                    pstmt.setString(26, updated_by);
                    if (rank_change != null) {
                        pstmt.setBoolean(27, rank_change.equals("1"));
                    }
                    else {
                        pstmt.setNull(27, java.sql.Types.BOOLEAN);
                    }
                    
                    if (epic != null) {
                        pstmt.setString(28, epic);
                    }
                    else {
                        pstmt.setNull(28, java.sql.Types.VARCHAR);
                    }
                    pstmt.setBoolean(29, flagged.equals("1"));
                    pstmt.setInt(30, Integer.parseInt(ready_status));
                    pstmt.setInt(31, Integer.parseInt(labels));

                    bstmt.batch();
                }
            }
            
            bstmt.execute();
        }
            
        catch (SQLException e) {
            printSQLExceptionDetails(e);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (bstmt != null) { bstmt.close(); }
            if (existsStmt != null) try { existsStmt.close(); } catch (SQLException e) {e.printStackTrace();}
            if (rs != null) try { rs.close(); } catch (SQLException e) {e.printStackTrace();}
        }
        
    }

}
    
