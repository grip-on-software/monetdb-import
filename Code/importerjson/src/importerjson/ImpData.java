/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importerjson;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Enrique
 */
public class ImpData {
    
    public void parser(){

        BufferedReader br = null;
        PreparedStatement pstmt = null;
        Connection con = null;
        Statement st = null;
        JSONParser parser = new JSONParser();
        
        //JSONParser parser = new JSONParser();
 
        try {
 
            
            Class.forName("nl.cwi.monetdb.jdbc.MonetDriver");
            con = DriverManager.getConnection("jdbc:monetdb://MONETDB_SERVER.localhost:50000/gros", "monetdb", "monetdb");
            
 
            JSONArray a = (JSONArray) parser.parse(new FileReader("/path/to/data.json"));
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String additional_information = (String) jsonObject.get("additional_information");
                String assignee = (String) jsonObject.get("assignee");
                String title = (String) jsonObject.get("title");
                String fixVersions = (String) jsonObject.get("fixVersions");
                String priority = (String) jsonObject.get("priority");
                String attachment = (String) jsonObject.get("attachment");
                String project_id = (String) jsonObject.get("project_id");
                String type = (String) jsonObject.get("type");
                String duedate = (String) jsonObject.get("duedate");
                String status = (String) jsonObject.get("status");
                String updated = (String) jsonObject.get("updated");           
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
                
                if ((resolution.trim()).equals("None") ){
                    resolution = "0";
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
                if ((bugfix.trim()).equals("None") ){
                    bugfix = "0";
                }
            
                /*
                System.out.println("additional_information: " + additional_information);
                System.out.println("assignee: " + assignee);
                System.out.println("title: " + title);
                System.out.println("fixVersions: " + fixVersions);
                System.out.println("priority: " + priority);
                System.out.println("attachment: " + attachment);
                System.out.println("project_id: " + project_id);
                System.out.println("type: " + type);
                System.out.println("duedate: " + duedate);
                System.out.println("status: " + status);
                System.out.println("updated: " + updated);
                System.out.println("description: " + description);
                System.out.println("reporter: " + reporter);
                System.out.println("key: " + key);
                System.out.println("resolution_date: " + resolution_date);
                System.out.println("storypoint: " + storypoint);
                System.out.println("watchers: " + watchers);
                System.out.println("created: " + created);
                System.out.println("bugfix: " + bugfix);
                System.out.println("review_comments: " + review_comments);
                System.out.println("issue_id: " + issue_id);
                System.out.println("resolution: " + resolution);
                */
                
                String sql = "insert into gros.issue values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
                
                pstmt = con.prepareStatement(sql);
                
                pstmt.setInt(1, Integer.parseInt(issue_id));
                pstmt.setString(2, key);
                pstmt.setString(3, title);
                pstmt.setInt(4, Integer.parseInt(type));
                pstmt.setInt(5, Integer.parseInt(priority));
                pstmt.setInt(6, Integer.parseInt(resolution));
                pstmt.setInt(7, Integer.parseInt(fixVersions));
                pstmt.setInt(8, Integer.parseInt(bugfix));
                pstmt.setInt(9, Integer.parseInt(watchers));
                
                //SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
                //Date dcreated = dateFormat.parse(created);
                //Timestamp ts_created = new java.sql.Timestamp(dcreated.getTime());
                
                //String date = "2009-07-16T19:20:30-05:00";
                //String pattern = "yyyy-MM-dd'T'HH:mm:ssZ";
                //DateTimeFormatter dtf = DateTimeFormat.forPattern(pattern);
                //DateTime dateTime = dtf.parseDateTime(date);
                //String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
                //DateTimeFormatter dtf = DateTimeFormat.forPattern(pattern);
                //DateTime dT_created = dtf.parseDateTime(created);
                //Timestamp ts_created = new Timestamp(dT_created.getMillis());//Timestamp.valueOf(created);
                Timestamp ts_created;              
                if (created !=null){
                    ts_created = Timestamp.valueOf(created); 
                    pstmt.setTimestamp(10,ts_created);
                } else{
                    //ts_created = null;
                    pstmt.setNull(10, java.sql.Types.TIMESTAMP);
                }
                       
                
                
                
                
                //Date dupdated = dateFormat.parse(updated);
                //Timestamp ts_updated = new java.sql.Timestamp(dupdated.getTime());
                
                //DateTime dT_updated = dtf.parseDateTime(updated);
                //Timestamp ts_updated = new Timestamp(dT_updated.getMillis());
                Timestamp ts_updated;              
                if (updated !=null){
                    ts_updated = Timestamp.valueOf(updated); 
                    pstmt.setTimestamp(11,ts_updated);
                } else{
                    //ts_updated = null;
                    pstmt.setNull(11, java.sql.Types.TIMESTAMP);
                }
                
                //Timestamp ts_updated = Timestamp.valueOf(updated);                
                //System.out.println(description);            
                pstmt.setString(12, description);
                
                //Date dduedate = dateFormat.parse(duedate);
                //Timestamp ts_duedate = new java.sql.Timestamp(dduedate.getTime());
                
                //DateTime dT_duedate = dtf.parseDateTime(duedate);
                //Timestamp ts_duedate = new Timestamp(dT_duedate.getMillis());
                
                Timestamp ts_duedate;              
                if (duedate !=null){
                    ts_duedate = Timestamp.valueOf(duedate); 
                    pstmt.setTimestamp(13,ts_duedate);
                } else{
                    //ts_duedate = null;
                    pstmt.setNull(13, java.sql.Types.TIMESTAMP);
                }
                
                //Timestamp ts_duedate = Timestamp.valueOf(duedate);
                
                
                pstmt.setInt(14, Integer.parseInt(project_id));
                pstmt.setInt(15, Integer.parseInt(status));
                pstmt.setString(16, "");
                pstmt.setString(17, reporter);
                pstmt.setString(18, assignee);
                pstmt.setInt(19, Integer.parseInt(attachment));
                pstmt.setString(20, additional_information);
                pstmt.setString(21, review_comments);
                pstmt.setInt(22, Integer.parseInt(storypoint));
                
                //Date dresdate = dateFormat.parse(resolution_date);
                //Timestamp ts_resolution_date = new java.sql.Timestamp(dresdate.getTime());
                //DateTime dT_resolution_date = dtf.parseDateTime(resolution_date);
                //Timestamp ts_resolution_date = new Timestamp(dT_resolution_date.getMillis());
                Timestamp ts_resolution_date;              
                if (resolution_date !=null){
                    
                    ts_resolution_date = Timestamp.valueOf(resolution_date); 
                    
                    pstmt.setTimestamp(23,ts_resolution_date);
                } else{
                    //ts_resolution_date = null;
                    pstmt.setNull(23, java.sql.Types.TIMESTAMP);
                }
                
                //Timestamp ts_resolution_date = Timestamp.valueOf(resolution_date);                
                
                
                pstmt.executeUpdate();
            }
            //con.commit(); 
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
        

}
    
