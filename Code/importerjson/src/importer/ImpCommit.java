/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.BatchedCheckStatement;
import dao.BatchedStatement;
import dao.DataSource;
import dao.DeveloperDb;
import dao.DeveloperDb.Developer;
import dao.RepositoryDb;
import dao.SaltDb;
import dao.SaltDb.Encryption;
import dao.SprintDb;
import java.beans.PropertyVetoException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import util.BaseImport;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;
import util.BufferedJSONReader;

/**
 * Importer for VCS commit versions and some global special tasks.
 * @author Thomas, Enrique
 */
public class ImpCommit extends BaseImport {
    
    @Override
    public void parser() {
        int projectID = getProjectID();
        String sql = "insert into gros.commits(version_id,project_id,commit_date,sprint_id,developer_id,message,size_of_commit,insertions,deletions,number_of_files,number_of_lines,type,repo_id,author_date,branch) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
 
        try (
            DeveloperDb devDb = new DeveloperDb();
            RepositoryDb repoDb = new RepositoryDb();
            SprintDb sprintDb = new SprintDb();
            FileReader fr = new FileReader(getPath()+getProjectName()+"/data_vcs_versions.json");
            BufferedJSONReader br = new BufferedJSONReader(fr);
            BatchedCheckStatement cstmt = new BatchedCheckStatement("gros.commits", sql,
                    new String[]{"version_id", "repo_id"},
                    new int[]{java.sql.Types.VARCHAR, java.sql.Types.INTEGER}
            ) {
                private final int projectID = getProjectID();
                
                @Override
                protected void addToBatch(Object[] values, Object data, PreparedStatement pstmt) throws SQLException, PropertyVetoException {
                    String version_id = (String)values[0];
                    int repo_id = (int)values[1];

                    JSONObject jsonObject = (JSONObject) data;
                    String commit_date = (String) jsonObject.get("commit_date");
                    String author_date = (String) jsonObject.get("author_date");
                    String sprint = (String) jsonObject.get("sprint_id");
                    String developer = (String) jsonObject.get("developer");
                    String developer_email = (String) jsonObject.get("developer_email");
                    String message = (String) jsonObject.get("message");
                    String size_of_commit = (String) jsonObject.get("size");
                    String insertions = (String) jsonObject.get("insertions");
                    String deletions = (String) jsonObject.get("deletions");
                    String number_of_files = (String) jsonObject.get("number_of_files");
                    String number_of_lines = (String) jsonObject.get("number_of_lines");
                    String type = (String) jsonObject.get("type");
                    String branch = (String) jsonObject.get("branch");
                    String encrypted = (String) jsonObject.get("encrypted");

                    int sprint_id;
                    if ((sprint.trim()).equals("null")) { // In case not in between dates of sprint
                        sprint_id = 0;
                    }
                    else {
                        sprint_id = Integer.parseInt(sprint);
                    }

                    if (developer.equals("unknown")) {
                        developer = developer_email;
                    }
                    if (developer_email.equals("0")) {
                        developer_email = null;
                    }
                    int encryption = Encryption.parseInt(encrypted);

                    Developer dev = new Developer(developer, developer_email);
                    int developer_id = devDb.update_vcs_developer(projectID, dev, encryption);

                    pstmt.setString(1, version_id);
                    pstmt.setInt(2, projectID);

                    Timestamp ts_created = Timestamp.valueOf(commit_date); 
                    pstmt.setTimestamp(3, ts_created);

                    if (sprint_id == 0) {
                        sprint_id = sprintDb.find_sprint(projectID, ts_created);
                    }

                    pstmt.setInt(4, sprint_id);

                    pstmt.setInt(5, developer_id);
                    pstmt.setString(6, message);
                    pstmt.setInt(7, Integer.parseInt(size_of_commit));
                    pstmt.setInt(8, Integer.parseInt(insertions));
                    pstmt.setInt(9, Integer.parseInt(deletions));
                    pstmt.setInt(10, Integer.parseInt(number_of_files));
                    pstmt.setInt(11, Integer.parseInt(number_of_lines));
                    pstmt.setString(12, type);
                    pstmt.setInt(13, repo_id);

                    if (author_date == null || author_date.equals("0")) {
                        pstmt.setNull(14, java.sql.Types.TIMESTAMP);
                    }
                    else {
                        Timestamp authored = Timestamp.valueOf(author_date);
                        pstmt.setTimestamp(14, authored);
                    }

                    if (branch == null || branch.equals("0")) {
                        pstmt.setNull(15, java.sql.Types.VARCHAR);
                    }
                    else {
                        pstmt.setString(15, branch);
                    }
                    
                    insertStmt.batch();
                }
            }
        ) {
            cstmt.setBatchSize(100);
            Object o;
            while ((o = br.readObject()) != null) {
                JSONObject jsonObject = (JSONObject) o;
                
                String version_id = (String) jsonObject.get("version_id");
                String repo_name = (String) jsonObject.get("repo_name");
                
                // Check if repo ID exists or create repo with new ID
                int repo_id = repoDb.check_repo(repo_name, projectID);
                if (repo_id == 0) {
                    repoDb.insert_repo(repo_name, projectID);
                    repo_id = repoDb.check_repo(repo_name, projectID);
                }
                
                Object values[] = new Object[]{version_id, repo_id};
                cstmt.batch(values, o);
            }
            
            cstmt.execute();
            
            //Used for creating Project if it didn't exist
            this.setProjectID(projectID);
        }
        catch (IOException | SQLException | PropertyVetoException | ParseException | NumberFormatException ex) {
            logException(ex);
        }
    }
    
    /**
     * Update links to sprints for rows that do not have a sprint ID.
     */
    public void updateSprintLink() {
        String sql = "select commits.project_id, commits.repo_id, commits.version_id, commits.commit_date from gros.commits join gros.sprint on commits.project_id = sprint.project_id and commits.commit_date between sprint.start_date and sprint.end_date where commits.sprint_id = 0";
        String updateSql = "update gros.commits set sprint_id = ? where project_id = ? and repo_id = ? and version_id = ?";
        int projectID = this.getProjectID();
        if (projectID != 0) {
            sql += " and commits.project_id = ?";
        }
 
        try (
            SprintDb sprintDb = new SprintDb();
            Connection con = DataSource.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql);
            BatchedStatement bstmt = new BatchedStatement(updateSql)
        ) {
            PreparedStatement updateStmt = bstmt.getPreparedStatement();
            
            if (projectID != 0) {
                pstmt.setInt(1, projectID);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int project_id = rs.getInt("project_id");
                    int repo_id = rs.getInt("repo_id");
                    String version_id = rs.getString("version_id");
                    Timestamp commit_date = rs.getTimestamp("commit_date");
                    int sprint_id = sprintDb.find_sprint(project_id, commit_date);
                    if (sprint_id != 0) {
                        updateStmt.setInt(1, sprint_id);
                        updateStmt.setInt(2, project_id);
                        updateStmt.setInt(3, repo_id);
                        updateStmt.setString(4, version_id);
                        bstmt.batch();
                    }
                }
            }
            
            bstmt.execute();
        }
        catch (SQLException | PropertyVetoException ex) {
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
        boolean success;
 
        try (
            FileReader fr = new FileReader(getRootPath()+"/data_vcsdev_to_dev.json");
            DeveloperDb devDb = new DeveloperDb();
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
                JSONObject jsonObject = (JSONObject) o;
                
                int jira_id;
                String name = (String) jsonObject.get("jira_user_name");
                String display_name = (String) jsonObject.get("display_name");
                String email = (String) jsonObject.get("email");
                Developer dev = new Developer(name, display_name, email);
                if (jsonObject.containsKey("id")) {
                    jira_id = Integer.parseInt((String) jsonObject.get("id"));
                    success = devDb.link_vcs_developer(projectID, jira_id, dev);
                    logLinkSuccess(jira_id, dev, success);
                }
                else if (jsonObject.containsKey("prefix")) {
                    String prefix = (String) jsonObject.get("prefix");
                    String pattern = (String) jsonObject.get("pattern");
                    String replace = (String) jsonObject.get("replace");
                    String mutate = (String) jsonObject.get("mutate");
                    if (pattern == null) {
                        pattern = Pattern.quote(prefix);
                        replace = "";
                    }
                    List<Developer> developers = devDb.search_vcs_developers(prefix + "%");
                    if (developers.isEmpty()) {
                        getLogger().log(Level.WARNING, "Prefix {0} did not match any VCS developers", prefix);
                    }
                    for (Developer matchDev : developers) {
                        String linkName = matchDev.getDisplayName().replaceFirst(pattern, replace);
                        if ("lower".equals(mutate)) {
                            linkName = linkName.toLowerCase();
                        }
                        Developer linkDev = new Developer(linkName, email);
                        jira_id = devDb.check_project_developer(projectID, linkDev, Encryption.NONE);
                        success = devDb.link_vcs_developer(projectID, jira_id, matchDev);
                        logLinkSuccess(jira_id, matchDev, success);
                    }
                }
                else {
                    jira_id = devDb.check_project_developer(projectID, dev, Encryption.NONE);
                    success = devDb.link_vcs_developer(projectID, jira_id, dev);
                    logLinkSuccess(jira_id, dev, success);
                }
            }
        }
        catch (FileNotFoundException ex) {
            getLogger().log(Level.WARNING, "Cannot link VCS developers to JIRA developers: {0}", ex.getMessage());
        }
        catch (IOException | SQLException | PropertyVetoException | ParseException | NumberFormatException ex) {
            logException(ex);
        }
    }
    
    private void logLinkSuccess(int jira_id, Developer dev, boolean success) {
        Object[] params = new Object[]{dev.getDisplayName(), dev.getEmail(), jira_id};
        if (success) {
            getLogger().log(Level.INFO, "Linked developer name: {0}, email: {1} to JIRA ID {2}", params);
        }
        else {
            getLogger().log(Level.WARNING, "Could not link developer name: {0}, email: {1} to JIRA ID {2}", params);
        }
    }
    
    public void showUnknownDevs() {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        
        try {
            con = DataSource.getInstance().getConnection();

            String sql = "SELECT display_name, email FROM gros.vcs_developer WHERE jira_dev_id=0;";

            getLogger().info("These VCS developers should be linked in to Jira. Add them to the file data_vcsdev_to_dev.json:");
            
            st = con.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                String display_name = rs.getString("display_name");
                String email = rs.getString("email");
                if (email == null) {
                    email = "NULL";
                }
                getLogger().log(Level.INFO, "Unknown VCS Developer: display name {0}, email {1}", new Object[]{display_name, email});
            }            
        } catch (PropertyVetoException | SQLException ex) {
            logException(ex);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ex) {logException(ex);}
            if (st != null) try { st.close(); } catch (SQLException ex) {logException(ex);}
            if (con != null) try { con.close(); } catch (SQLException ex) {logException(ex);}
        }
    }
    
    /**
     * Encrypts rows in tables that include sensitive data in some fields.
     */
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
        
        hashKeys.put("issue", new String[]{"issue_id", "changelog_id"});
        hashKeys.put("comment", new String[]{"comment_id"});
        
        // Fields to hash
        HashMap<String, String[]> hashFields = new HashMap<>();
        hashFields.put("vcs_developer", new String[]{"display_name", "email"});
        hashFields.put("developer", new String[]{"name", "display_name", "email"});
        
        hashFields.put("metric_version", new String[]{"developer"});
        hashFields.put("reservation", new String[]{"requester"});
        
        hashFields.put("issue", new String[]{"reporter", "assignee", "updated_by"});
        hashFields.put("comment", new String[]{"author", "updater"});
        
        String[] tables;

        String encryptTables = System.getProperty("importer.encrypt_tables", "").trim();
        List<String> encryptionLevels = new ArrayList<>();
        encryptionLevels.add(String.valueOf(Encryption.NONE));
        if (encryptTables.isEmpty()) {
            if (projectID == 0) {
                tables = new String[]{"developer", "metric_version"};
            }
            else {
                tables = new String[]{"vcs_developer", "issue", "comment"};
            }
        }
        else {
            tables = encryptTables.split(",");
            if (projectID == 0) {
                // Project-then-global encryption
                encryptionLevels.add(String.valueOf(Encryption.PROJECT));
            }
        }
        
        try (SaltDb saltDb = new SaltDb()) {
            pair = saltDb.get_salt(projectID);
            
            con = DataSource.getInstance().getConnection();
            
            for (String table : tables) {
                if (!hashKeys.containsKey(table)) {
                    throw new RuntimeException("Table " + table + " cannot be encrypted");
                }
                String[] keys = hashKeys.get(table);
                String[] fields = hashFields.get(table);
                String selectSql = "SELECT " + String.join(", ", keys) + ", " + String.join(", ", fields) + ", encryption FROM gros." + table + " WHERE encryption IN (" + String.join(", ", encryptionLevels) + ")";

                st = con.createStatement();
                rs = st.executeQuery(selectSql);
                String updateSql = "UPDATE gros." + table + " SET " + String.join("=?, ", fields) + "=?, encryption=? WHERE " + String.join("=? AND ", keys) + "=? AND encryption=?";

                try (BatchedStatement bstmt = new BatchedStatement(updateSql)) {
                    PreparedStatement pstmt = bstmt.getPreparedStatement();
                    while (rs.next()) {
                        int index = 1;
                        for (String field : fields) {
                            String value = rs.getString(field);
                            String hashValue = saltDb.hash(value, pair);
                            
                            setString(pstmt, index, hashValue);
                            index++;
                        }
                        int encryption = rs.getInt("encryption");
                        pstmt.setInt(index, Encryption.add(encryption, projectID == 0 ? Encryption.GLOBAL : Encryption.PROJECT));
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
                        pstmt.setInt(index, encryption);
                        
                        bstmt.batch();
                    }
                    
                    bstmt.execute();
                }
                
                getLogger().log(Level.INFO, "Encrypted fields in {0} table", table);

            }
        } catch (PropertyVetoException | SQLException ex) {
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
                Developer dev = new Developer(name, display_name, email);
                
                devDb.insert_project_developer(project_id, id, dev);
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
        return "version control system commit versions";
    }

}
    
