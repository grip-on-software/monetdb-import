/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

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

/**
 *
 * @author Thomas and Enrique
 */
public class ImpCommit extends BaseImport{
    
    public void parser(int projectID, String projectN){

        BufferedReader br = null;
        PreparedStatement pstmt = null;
        Connection con = null;
        Statement st = null;
        JSONParser parser = new JSONParser();
        ResultSet rs = null;
        DeveloperDb devDb = new DeveloperDb();
        RepositoryDb repoDb = new RepositoryDb();
 
        try {
            
            con = DataSource.getInstance().getConnection();
            
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+projectN+"/data_commits.json"));
            
            for (Object o : a)
            {
                JSONObject jsonObject = (JSONObject) o;
                
                String commit_id = (String) jsonObject.get("commit_id");
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
                String git_repo = (String) jsonObject.get("git_repo");
                
                if ((sprint_id.trim()).equals("null") ){ // In case not in between dates of sprint
                    sprint_id = "0";
                }
                if (developer.equals("unknown")) {
                    developer = developer_email;
                }
                developer = addSlashes(developer);
                int developer_id = devDb.check_developer_git(developer);
                
                if (developer_id == 0) { // if developer id does not exist, create developer with new id and _git behind name
                    int dev_id = devDb.check_developer(developer); // Check if developer already exists in developer table      
                    devDb.insert_developer_git(dev_id, developer); // if dev id = 0 then link later.
                    developer_id = devDb.check_developer_git(developer); // set new id of dev
                }
                
                int repo_id = repoDb.check_repo(git_repo);
                
                if (developer_id == 0) { // if developer id does not exist, create developer with new id and _git behind name  
                    repoDb.insert_repo(git_repo); // if dev id = 0 then link later.
                    repo_id = devDb.check_developer_git(git_repo); // set new id of dev
                }

                String sql = "insert into gros.commits values (?,?,?,?,?,?,?,?,?,?,?,?,?);";
                
                pstmt = con.prepareStatement(sql);
                
                pstmt.setString(1, commit_id);
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
                /* message = this.addSlashes(message);
                String new_message = message.replace("'", "\\'"); */
                pstmt.setString(6, message);
                pstmt.setInt(7, Integer.parseInt(size_of_commit));
                pstmt.setInt(8, Integer.parseInt(insertions));
                pstmt.setInt(9, Integer.parseInt(deletions));
                pstmt.setInt(10, Integer.parseInt(number_of_files));
                pstmt.setInt(11, Integer.parseInt(number_of_lines));
                pstmt.setString(12, type);
                pstmt.setInt(13, repo_id);

                pstmt.executeUpdate();
            }
            
            //Used for creating Project if it didn't exist
            this.setProjectID(projectID);
                  
        }
            
        catch (IOException | SQLException | PropertyVetoException | ParseException | NumberFormatException e) {
            e.printStackTrace();
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {e.printStackTrace();}
            if (st != null) try { st.close(); } catch (SQLException e) {e.printStackTrace();}
            if (con != null) try { con.close(); } catch (SQLException e) {e.printStackTrace();}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {e.printStackTrace();}
        }
        
    }
    
    /**
     * Updates the JiraID's of the git developer table. It uses a json file to read
     * all the aliases found on Git and then links them to the JiraID's. Best is to do this
     * after collecting all the records of all the projects.
     * @param projectID
     * @param projectN 
     */
    public void updateJiraID(int projectID, String projectN) {
        BufferedReader br = null;
        PreparedStatement pstmt = null;
        Connection con = null;
        Statement st = null;
        JSONParser parser = new JSONParser();
        ResultSet rs = null;
        //DeveloperDb devDb = new DeveloperDb();
 
        try {
            con = DataSource.getInstance().getConnection();
            
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+projectN+"/data_gitdev_to_dev.json"));
            
            for (Object o : a)
            {
                JSONObject jsonObject = (JSONObject) o;
                
                String display_name = (String) jsonObject.get("display_name");
                String jira_dev_id = (String) jsonObject.get("jira_dev_id");
                
                String sql = "UPDATE gros.git_developer SET jira_dev_id=? WHERE display_name=?;";
                
                pstmt = con.prepareStatement(sql);
                
                pstmt.setInt(1, Integer.parseInt(jira_dev_id));
                pstmt.setString(2, display_name);

                pstmt.executeUpdate();
            }
            
            //Used for creating Project if it didn't exist
            this.setProjectID(projectID);
                  
        }
            
        catch (IOException | SQLException | PropertyVetoException | ParseException | NumberFormatException e) {
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
     * @param projectID Project ID
     * @param ProjectN Project name
     */
    public void updateDevelopers(int projectID, String projectN) {
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
    
