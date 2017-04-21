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
import java.io.UnsupportedEncodingException;
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
        String sql = "insert into gros.commits values (?,?,?,?,?,?,?,?,?,?,?,?,?);";
 
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
                
                if ((sprint_id.trim()).equals("null") ){ // In case not in between dates of sprint
                    sprint_id = "0";
                }
                
                if (developer.equals("unknown")) {
                    developer = developer_email;
                }
                if (developer_email.equals("0")) {
                    developer_email = null;
                }
                
                int developer_id = devDb.update_vcs_developer(developer, developer_email);
                
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
        PreparedStatement pstmt = null;
        Connection con = null;
        PreparedStatement selectStmt = null;
        JSONParser parser = new JSONParser();
        ResultSet rs = null;
        //DeveloperDb devDb = new DeveloperDb();
 
        try (FileReader fr = new FileReader(getRootPath()+"/data_vcsdev_to_dev.json")) {
            con = DataSource.getInstance().getConnection();

            String sql = "SELECT id FROM gros.developer WHERE name = ?;";
            selectStmt = con.prepareStatement(sql);

            sql = "UPDATE gros.vcs_developer SET jira_dev_id=? WHERE display_name=?;";
            pstmt = con.prepareStatement(sql);
            
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
                JSONObject jsonObject = (JSONObject) o;
                
                String display_name = (String) jsonObject.get("display_name");
                String jira_user_name = (String) jsonObject.get("jira_user_name");
                
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
            
        catch (IOException | SQLException | PropertyVetoException | ParseException | NumberFormatException ex) {
            logException(ex);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ex) {logException(ex);}
            if (selectStmt != null) try { selectStmt.close(); } catch (SQLException ex) {logException(ex);}
            if (con != null) try { con.close(); } catch (SQLException ex) {logException(ex);}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException ex) {logException(ex);}
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
    
    private static String sha256(String base) {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();

            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch(UnsupportedEncodingException | NoSuchAlgorithmException ex){
           throw new RuntimeException(ex);
        }
    }
    
    public void hashNames() {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        String salt;
        String pepper;

        try (SaltDb saltDb = new SaltDb()) {
            SaltDb.SaltPair pair = saltDb.get_salt(this.getProjectID());
            salt = pair.getSalt();
            pepper = pair.getPepper();
        }
        catch (PropertyVetoException | IOException | SQLException ex) {
            logException(ex);
            return;
        }
        
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
        
        try {
            con = DataSource.getInstance().getConnection();
            
            for (String table : hashKeys.keySet()) {
                String[] keys = hashKeys.get(table);
                String[] fields = hashFields.get(table);
                String selectSql = "SELECT " + String.join(", ", keys) + ", " + String.join(", ", fields) + " FROM gros." + table + " WHERE encrypted=false";

                st = con.createStatement();
                rs = st.executeQuery(selectSql);
                String updateSql = "UPDATE gros." + table + " SET " + String.join("=?, ", fields) + "=?, encrypted=true WHERE " + String.join("=? AND ", keys) + "=? AND encrypted=false";

                try (BatchedStatement bstmt = new BatchedStatement(updateSql)) {
                    PreparedStatement pstmt = bstmt.getPreparedStatement();
                    while (rs.next()) {
                        int index = 1;
                        for (String field : fields) {
                            String value = rs.getString(field);
                            String hashValue = sha256(salt + value + pepper);
                            
                            pstmt.setString(index, hashValue);
                            index++;
                        }
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
     * Updates the entire commit table with the right jira dev id's instead of
     * the alias ids. ONLY RUN THIS IF THE GIT DEVELOPER TABLE IS COMPLETELY UPDATED!
     */
    public void updateDevelopers() {
        DeveloperDb devDb = new DeveloperDb();
 
        devDb.updateCommits();
    }
    
    @Override
    public String getImportName() {
        return "VCS commit versions";
    }

}
    
