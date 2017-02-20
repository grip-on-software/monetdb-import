/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.BatchedStatement;
import dao.DataSource;
import dao.DeveloperDb;
import dao.RepositoryDb;
import java.beans.PropertyVetoException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import util.BaseImport;
import java.security.*;

/**
 *
 * @author Thomas and Enrique
 */
public class ImpCommit extends BaseImport{
    
    @Override
    public void parser() {

        BatchedStatement bstmt = null;
        PreparedStatement pstmt = null;
        JSONParser parser = new JSONParser();
        DeveloperDb devDb = new DeveloperDb();
        RepositoryDb repoDb = new RepositoryDb();
        int projectID = getProjectID();
 
        try {
            String sql = "insert into gros.commits values (?,?,?,?,?,?,?,?,?,?,?,?,?);";
            bstmt = new BatchedStatement(sql);
                
            pstmt = bstmt.getPreparedStatement();
            
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+getProjectName()+"/data_vcs_versions.json"));
            
            for (Object o : a)
            {
                JSONObject jsonObject = (JSONObject) o;
                
                String version_id = (String) jsonObject.get("version_id");
                String commit_date = (String) jsonObject.get("commit_date");
                String sprint_id = (String) jsonObject.get("sprint_id").toString();
                String developer = (String) jsonObject.get("developer");
                String developer_email = (String) jsonObject.get("developer_email");
                String message = (String) jsonObject.get("message");
                String size_of_commit = (String) jsonObject.get("size_of_commit");
                String insertions = (String) jsonObject.get("insertions");
                String deletions = (String) jsonObject.get("deletions");
                String number_of_files = (String) jsonObject.get("number_of_files");
                String number_of_lines = (String) jsonObject.get("number_of_lines");
                String type = (String) jsonObject.get("type");
                String repo_name = (String) jsonObject.get("repo_name");
                
                if ((sprint_id.trim()).equals("null") ){ // In case not in between dates of sprint
                    sprint_id = "0";
                }
                
                if (developer.equals("unknown")) {
                    developer = developer_email;
                }
                
                int developer_id = devDb.check_vcs_developer(developer);
                if (developer_id == 0) { // if developer id does not exist, create VCS developer with new id
                    // Check if developer exists with the same (short) name in JIRA developer table
                    int dev_id = devDb.check_developer(developer, developer);
                    devDb.insert_vcs_developer(dev_id, developer); // if dev id = 0 then link later.
                    developer_id = devDb.check_vcs_developer(developer); // set new id of dev
                }
                
                int repo_id = repoDb.check_repo(repo_name);
                
                if (repo_id == 0) { // if repo id does not exist, create repo with new id
                    repoDb.insert_repo(repo_name);
                    repo_id = repoDb.check_repo(repo_name); // set new id of repo
                }
                
                pstmt.setString(1, version_id);
                pstmt.setInt(2, projectID);

                Timestamp ts_created;              
                if (commit_date != null){
                    ts_created = Timestamp.valueOf(commit_date); 
                    pstmt.setTimestamp(3,ts_created);
                } else{
                    //ts_created = null;
                    pstmt.setNull(3, java.sql.Types.TIMESTAMP);
                }

                pstmt.setInt(4, Integer.parseInt(sprint_id));

                // Calculate developerid Int or String?
                pstmt.setInt(5, developer_id);
                pstmt.setString(6, message);
                pstmt.setInt(7, Integer.parseInt(size_of_commit));
                pstmt.setInt(8, Integer.parseInt(insertions));
                pstmt.setInt(9, Integer.parseInt(deletions));
                pstmt.setInt(10, Integer.parseInt(number_of_files));
                pstmt.setInt(11, Integer.parseInt(number_of_lines));
                pstmt.setString(12, type);
                pstmt.setInt(13, repo_id);

                bstmt.batch();
            }
            
            bstmt.execute();
            
            //Used for creating Project if it didn't exist
            this.setProjectID(projectID);
            
            devDb.close();
            repoDb.close();
        }
            
        catch (IOException | SQLException | PropertyVetoException | ParseException | NumberFormatException e) {
            e.printStackTrace();
        } finally {
            if (bstmt != null) { bstmt.close(); }
        }
        
    }
    
    /**
     * Updates the JiraID's of the VCS developer table. It uses a json file to read
     * all the aliases found on the version control system and then links them to the JiraID's.
     * Best is to do this after collecting all the records of all the projects.
     */
    public void updateJiraID() {
        BufferedReader br = null;
        PreparedStatement pstmt = null;
        Connection con = null;
        PreparedStatement selectStmt = null;
        JSONParser parser = new JSONParser();
        ResultSet rs = null;
        //DeveloperDb devDb = new DeveloperDb();
 
        try {
            con = DataSource.getInstance().getConnection();

            String sql = "SELECT id FROM gros.developer WHERE name = ?;";
            selectStmt = con.prepareStatement(sql);

            sql = "UPDATE gros.vcs_developer SET jira_dev_id=? WHERE display_name=?;";
            pstmt = con.prepareStatement(sql);
            
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+"/data_vcsdev_to_dev.json"));
            
            for (Object o : a)
            {
                JSONObject jsonObject = (JSONObject) o;
                
                String display_name = (String) jsonObject.get("display_name");
                String jira_user_name = (String) jsonObject.get("jira_user_name");
                display_name = addSlashes(display_name);                
                
                selectStmt.setString(1, jira_user_name);
                rs = selectStmt.executeQuery();
                int jira_id = 0;
                while (rs.next()) {
                    jira_id = (rs.getInt("id"));
                }
                                
                pstmt.setInt(1, jira_id);
                pstmt.setString(2, display_name);

                pstmt.executeUpdate();
            }
            
                  
        }
            
        catch (IOException | SQLException | PropertyVetoException | ParseException | NumberFormatException e) {
            e.printStackTrace();
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {e.printStackTrace();}
            if (selectStmt != null) try { selectStmt.close(); } catch (SQLException e) {e.printStackTrace();}
            if (con != null) try { con.close(); } catch (SQLException e) {e.printStackTrace();}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {e.printStackTrace();}
        }
    }
    
    public void printUnknownDevs() {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        
        try {
            con = DataSource.getInstance().getConnection();

            String sql = "SELECT display_name FROM gros.vcs_developer WHERE jira_dev_id=0;";

            System.out.println("These VCS developers should be linked in to Jira. Add them to the file data_vcsdev_to_dev.json: ");
            
            st = con.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                System.out.println("Unknown VCS Developer: " + rs.getString("display_name"));
            }
            
        } catch (SQLException e) {
            printSQLExceptionDetails(e);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {e.printStackTrace();}
            if (st != null) try { st.close(); } catch (SQLException e) {e.printStackTrace();}
            if (con != null) try { con.close(); } catch (SQLException e) {e.printStackTrace();}
        }
    }
    
    private static String sha256(String base) {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch(Exception ex){
           throw new RuntimeException(ex);
        }
    }
    
    public void hashNames() {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        String salt = "YOUR_SECURE_SALT_HERE";
        String pepper = "YOUR_SECURE_PEPPER_HERE";
        
        try {
            con = DataSource.getInstance().getConnection();

            /* Hash Git developers */
            String sql = "SELECT * FROM gros.vcs_developer";
            st = con.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                int alias_id = rs.getInt("alias_id");
                String dev_name = rs.getString("display_name");
                dev_name = sha256(salt + dev_name + pepper);
                
                String sql2 = "UPDATE gros.vcs_developer SET display_name='" + dev_name + "' WHERE alias_id=" + alias_id;
                st = con.createStatement();
                st.executeQuery(sql2);
                
            }
            
            /* Hash Jira Developers */
            sql = "SELECT * FROM gros.developer";
            st = con.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                int alias_id = rs.getInt("alias_id");
                String dev_name = rs.getString("display_name");
                String user_name = rs.getString("name");
                
                dev_name = sha256(salt + dev_name + pepper);
                user_name = sha256(salt + user_name + pepper);
                
                String sql2 = "UPDATE gros.developer SET name='" + user_name + "', display_name='" + dev_name + "' WHERE alias_id=" + alias_id;
                st = con.createStatement();
                st.executeQuery(sql2);
            }
            
            /* Hash Developers in Issue */
            sql = "SELECT * FROM gros.issue";
            st = con.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                int issue_id = rs.getInt("issue_id");
                Timestamp ts = rs.getTimestamp("updated_by");
                String reporter = rs.getString("reporter");
                String assignee = rs.getString("assignee");
                String updated_by = rs.getString("updated_by");
                
                if(!reporter.equals("None")) { 
                    reporter = sha256(salt + reporter + pepper);
                }
                if(!assignee.equals("None")) { 
                    assignee = sha256(salt + assignee + pepper);
                }
                if(!updated_by.equals("None")) { 
                    updated_by = sha256(salt + updated_by + pepper);
                }
                
                String sql2 = "UPDATE gros.issue SET reporter='" + reporter + "', assignee='" + assignee + "', '" + updated_by + "' WHERE issue_id=" + issue_id + " AND updated='" + ts + "'";
                st = con.createStatement();
                st.executeQuery(sql2);
            }
            
            /* Hash Developers in Comment*/
            sql = "SELECT * FROM gros.comment";
            st = con.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                int comment_id = rs.getInt("comment_id");
                String author = rs.getString("author");
                
                author = sha256(salt + author + pepper);
                
                String sql2 = "UPDATE gros.comment SET author='" + author + "' WHERE comment_id=" + comment_id;
                st = con.createStatement();
                st.executeQuery(sql2);
            }
            
            System.out.println("Developers hashed.");
            
        } catch (SQLException e) {
            printSQLExceptionDetails(e);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {e.printStackTrace();}
            if (st != null) try { st.close(); } catch (SQLException e) {e.printStackTrace();}
            if (con != null) try { con.close(); } catch (SQLException e) {e.printStackTrace();}
        }
        
    }
    
    /**
     * Updates the entire commit table with the right jira dev id's instead of
     * the alias ids. ONLY RUN THIS IF THE GIT DEVELOPER TABLE IS COMPLETELY UPDATED!
     */
    public void updateDevelopers() {
        DeveloperDb devDb = new DeveloperDb();
 
        devDb.updateCommits();
    }
    
    public static String addSlashes(String s) {
        s = s.replaceAll("\\\\", "\\\\\\\\");
        s = s.replaceAll("\\n", "\\\\n");
        s = s.replaceAll("\\r", "\\\\r");
        s = s.replaceAll("\\00", "\\\\0");
        s = s.replaceAll("'", "\\\\'");
        return s;
    }
        

}
    
