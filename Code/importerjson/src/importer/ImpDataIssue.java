/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.BatchedStatement;
import util.BaseImport;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Enrique
 */
public class ImpDataIssue extends BaseImport{
    
    public void parser(int projectId, String projectN){

        BufferedReader br = null;
        BatchedStatement bstmt = null;
        PreparedStatement pstmt = null;
        JSONParser parser = new JSONParser();
        String new_description = "";
        
        try {                
            String sql = "insert into gros.issue values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";    
            bstmt = new BatchedStatement(sql);
            pstmt = bstmt.getPreparedStatement();
                
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+projectN+"/data.json"));
            
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
                if ((storypoint.trim()).equals("None")){
                    storypoint = "0";
                }
                if ((bugfix.trim()).equals("None")){
                    bugfix = "0";
                }
                
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
                    //ts_created = null;
                    pstmt.setNull(11, java.sql.Types.TIMESTAMP);
                }
                       
                Timestamp ts_updated;              
                if (updated != null){
                    ts_updated = Timestamp.valueOf(updated); 
                    pstmt.setTimestamp(12, ts_updated);
                } else{
                    //ts_updated = null;
                    pstmt.setNull(12, java.sql.Types.TIMESTAMP);
                }
                                 
                new_description = description.replace("'","\\'");
                pstmt.setString(13, new_description);
                
                Timestamp ts_duedate;              
                if (duedate != null){
                    ts_duedate = Timestamp.valueOf(duedate); 
                    pstmt.setTimestamp(14, ts_duedate);
                } else{
                    //ts_duedate = null;
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
                pstmt.setInt(23, Integer.parseInt(storypoint));
                
                Timestamp ts_resolution_date;              
                if (resolution_date != null){
                    
                    ts_resolution_date = Timestamp.valueOf(resolution_date); 
                    
                    pstmt.setTimestamp(24, ts_resolution_date);
                } else{
                    //ts_resolution_date = null;
                    pstmt.setNull(24, java.sql.Types.TIMESTAMP);
                }

                pstmt.setInt(25, Integer.parseInt(sprint));                
                
                if (updated_by == "") {
                    updated_by = reporter;
                }
                
                pstmt.setString(26, updated_by);
                
                bstmt.batch();
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
            bstmt.close();
        }
        
    }

}
    
