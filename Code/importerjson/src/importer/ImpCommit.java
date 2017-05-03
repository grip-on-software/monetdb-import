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
import dao.SaltDb;
import java.beans.PropertyVetoException;
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
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.BufferedJSONReader;

/**
 *
 * @author Thomas and Enrique
 */
public class ImpCommit extends BaseImport{
    
    @Override
    public void parser() {
        int projectID = getProjectID();
        String sql = "insert into gros.commits(version_id,project_id,commit_date,sprint_id,developer_id,message,size_of_commit,insertions,deletions,number_of_files,number_of_lines,type,repo_id) values (?,?,?,?,?,?,?,?,?,?,?,?,?);";
 
        try (
            DeveloperDb devDb = new DeveloperDb();
            RepositoryDb repoDb = new RepositoryDb();
            FileReader fr = new FileReader(getPath()+getProjectName()+"/data_vcs_versions.json");
            BufferedJSONReader br = new BufferedJSONReader(fr);
            BatchedStatement bstmt = new BatchedStatement(sql)
        ) {
                
            PreparedStatement pstmt = bstmt.getPreparedStatement();
            
            Object o;
            while ((o = br.readObject()) != null) {
                JSONObject jsonObject = (JSONObject) o;
                
                String version_id = (String) jsonObject.get("version_id");
                String commit_date = (String) jsonObject.get("commit_date");
                String sprint_id = (String) jsonObject.get("sprint_id").toString();
                String developer = (String) jsonObject.get("developer");
                String developer_email = (String) jsonObject.get("developer_email");
                String message = (String) jsonObject.get("message");
                String size_of_commit = (String) jsonObject.get("size");
                String insertions = (String) jsonObject.get("insertions");
                String deletions = (String) jsonObject.get("deletions");
                String number_of_files = (String) jsonObject.get("number_of_files");
                String number_of_lines = (String) jsonObject.get("number_of_lines");
                String type = (String) jsonObject.get("type");
                String repo_name = (String) jsonObject.get("repo_name");
                String encrypted = (String) jsonObject.get("encrypted");
                
                if ((sprint_id.trim()).equals("null") ){ // In case not in between dates of sprint
                    sprint_id = "0";
                }
                
                if (developer.equals("unknown")) {
                    developer = developer_email;
                }
                if (developer_email.equals("0")) {
                    developer_email = null;
                }
                int encryption = SaltDb.Encryption.parseInt(encrypted);
                
                int developer_id = devDb.update_vcs_developer(projectID, developer, developer_email, encryption);
                
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
        }
        catch (IOException | SQLException | PropertyVetoException | ParseException | NumberFormatException ex) {
            logException(ex);
        }
    }
    
    /**
     * Updates the JiraID's of the VCS developer table. It uses a json file to read
     * all the aliases found on the version control system and then links them to the JiraID's.
     * Best is to do this after collecting all the records of all the projects.
     */
    public void updateJiraID() {
        JSONParser parser = new JSONParser();
        int projectID = this.getProjectID();
 
        String condition = "(encryption=? AND (display_name=? OR (email IS NOT NULL AND email=?)))";
        String sql = "UPDATE gros.vcs_developer SET jira_dev_id=? WHERE (" + condition + " OR " + condition + ");";
        try (
            FileReader fr = new FileReader(getRootPath()+"/data_vcsdev_to_dev.json");
            DeveloperDb devDb = new DeveloperDb();
            SaltDb saltDb = new SaltDb();
            BatchedStatement bstmt = new BatchedStatement(sql)
        ) {
            SaltDb.SaltPair pair = saltDb.get_salt(projectID);
            PreparedStatement pstmt = bstmt.getPreparedStatement();
            
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
                JSONObject jsonObject = (JSONObject) o;
                
                int jira_id = 0;
                String display_name = (String) jsonObject.get("display_name");
                String email = (String) jsonObject.get("email");
                if (jsonObject.containsKey("id")) {
                    jira_id = Integer.parseInt((String) jsonObject.get("id"));
                }
                else if (projectID == 0) {
                    jira_id = devDb.check_developer(null, display_name, email);
                }
                else {
                    jira_id = devDb.check_project_developer(projectID, display_name, email, SaltDb.Encryption.NONE);
                }
                
                if (jira_id != 0) {
                    pstmt.setInt(1, jira_id);
                    
                    pstmt.setInt(2, SaltDb.Encryption.NONE);
                    pstmt.setString(3, display_name);
                    setString(pstmt, 4, email);
                    
                    pstmt.setInt(5, projectID == 0 ? SaltDb.Encryption.GLOBAL : SaltDb.Encryption.PROJECT);
                    pstmt.setString(6, saltDb.hash(display_name, pair));
                    setString(pstmt, 7, saltDb.hash(email, pair));

                    bstmt.batch();
                }
            }
            
            bstmt.execute();
        }
        catch (IOException | SQLException | PropertyVetoException | ParseException | NumberFormatException ex) {
            logException(ex);
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
        } catch (PropertyVetoException | IOException | SQLException ex) {
            logException(ex);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ex) {logException(ex);}
            if (st != null) try { st.close(); } catch (SQLException ex) {logException(ex);}
            if (con != null) try { con.close(); } catch (SQLException ex) {logException(ex);}
        }
    }
    
    public void hashNames() {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        SaltDb.SaltPair pair;
        int projectID = getProjectID();
        
        // Primary keys of the tables to update
        HashMap<String, String[]> hashKeys  = new HashMap<>();
        hashKeys.put("vcs_developer", new String[]{"alias_id"});
        hashKeys.put("developer", new String[]{"id"});
        
        hashKeys.put("metric_version", new String[]{"project_id", "version_id"});
        hashKeys.put("reservation", new String[]{"reservation_id"});
        
        hashKeys.put("merge_request", new String[]{"repo_id", "request_id"});
        hashKeys.put("merge_request_note", new String[]{"repo_id", "request_id", "note_id"});
        hashKeys.put("commit_comment", new String[]{"repo_id", "version_id", "author", "comment", "file", "line", "line_type"});
        
        hashKeys.put("issue", new String[]{"issue_id", "changelog_id"});
        hashKeys.put("comment", new String[]{"comment_id"});
        
        // Fields to hash
        HashMap<String, String[]> hashFields = new HashMap<>();
        hashFields.put("vcs_developer", new String[]{"display_name", "email"});
        hashFields.put("developer", new String[]{"name", "display_name", "email"});
        
        hashFields.put("metric_version", new String[]{"developer"});
        hashFields.put("reservation", new String[]{"requester"});

        hashFields.put("merge_request", new String[]{"author", "assignee"});
        hashFields.put("merge_request_note", new String[]{"author"});
        hashFields.put("commit_comment", new String[]{"author"});
        
        hashFields.put("issue", new String[]{"reporter", "assignee", "updated_by"});
        hashFields.put("comment", new String[]{"author"});
        
        try (SaltDb saltDb = new SaltDb()) {
            pair = saltDb.get_salt(this.getProjectID());
            
            con = DataSource.getInstance().getConnection();
            
            for (String table : hashKeys.keySet()) {
                String[] keys = hashKeys.get(table);
                String[] fields = hashFields.get(table);
                String selectSql = "SELECT " + String.join(", ", keys) + ", " + String.join(", ", fields) + " FROM gros." + table + " WHERE encryption=" + SaltDb.Encryption.NONE;

                st = con.createStatement();
                rs = st.executeQuery(selectSql);
                String updateSql = "UPDATE gros." + table + " SET " + String.join("=?, ", fields) + "=?, encryption=? WHERE " + String.join("=? AND ", keys) + "=? AND encryption=" + SaltDb.Encryption.NONE;

                try (BatchedStatement bstmt = new BatchedStatement(updateSql)) {
                    PreparedStatement pstmt = bstmt.getPreparedStatement();
                    while (rs.next()) {
                        int index = 1;
                        for (String field : fields) {
                            String value = rs.getString(field);
                            String hashValue = saltDb.hash(value, pair);
                            
                            pstmt.setString(index, hashValue);
                            index++;
                        }
                        pstmt.setInt(index, projectID == 0 ? SaltDb.Encryption.GLOBAL : SaltDb.Encryption.PROJECT);
                        index++;
                        for (String key : keys) {
                            Object keyValue = rs.getObject(key);
                            if (keyValue instanceof Integer) {
                                pstmt.setInt(index, (int)keyValue);
                            }
                            else {
                                pstmt.setObject(index, keyValue);
                            }
                            index++;
                        }
                        bstmt.batch();
                    }
                    
                    bstmt.execute();
                }
                
                Logger.getLogger("importer").log(Level.INFO, "Encrypted fields in {0} table", table);

            }
        } catch (PropertyVetoException | IOException | SQLException ex) {
            logException(ex);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ex) {logException(ex);}
            if (st != null) try { st.close(); } catch (SQLException ex) {logException(ex);}
            if (con != null) try { con.close(); } catch (SQLException ex) {logException(ex);}
        }
        
    }
    
    /**
     * Generate a table of project-specific developers from the JIRA and VCS
     * developers tables.
     * This project-specific table is to be used for matching developers once
     * they are encrypted.
     */
    public void fillProjectDevelopers() {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        
        try (DeveloperDb devDb = new DeveloperDb()) {
            con = DataSource.getInstance().getConnection();
            
            String selectSql = "SELECT developer.id, developer.name, developer.display_name, developer.email, issue.project_id FROM gros.developer JOIN gros.issue ON (developer.name = issue.updated_by OR developer.name = issue.reporter OR developer.name = issue.assignee) WHERE developer.encryption = 0 GROUP BY developer.id, developer.name, developer.display_name, developer.email, issue.project_id";
            st = con.createStatement();
            rs = st.executeQuery(selectSql);
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String display_name = rs.getString("display_name");
                String email = rs.getString("email");
                int project_id = rs.getInt("project_id");
                
                devDb.insert_project_developer(project_id, id, name, display_name, email);
            }
        }
        catch (Exception ex) {
            logException(ex);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ex) {logException(ex);}
            if (st != null) try { st.close(); } catch (SQLException ex) {logException(ex);}
            if (con != null) try { con.close(); } catch (SQLException ex) {logException(ex);}
        }
    }
        
    @Override
    public String getImportName() {
        return "VCS commit versions";
    }

}
    
