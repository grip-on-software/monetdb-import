/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

/**
 *
 * @author Enrique
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ResourceBundle;


public abstract class BaseImport {
    
    private String url;
    private String user;
    private String password;
    private String path;
    private String project;
    private int projectID = 0;
    
    public BaseImport() {
        ResourceBundle bundle = ResourceBundle.getBundle("util.import");
        url = bundle.getString("url").trim();
        user = bundle.getString("user").trim();
        password = bundle.getString("password").trim();
        path = bundle.getString("path").trim();
        project = bundle.getString("project").trim(); 
        //projectID = this.getProjectId(project);
     
    }
    
    private int getProjectId(String projectName){
    
        int idProject = 0;
        PreparedStatement pstmt = null;
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        
        try{
            
            Class.forName("nl.cwi.monetdb.jdbc.MonetDriver");
            con = DriverManager.getConnection(getUrl(), getUser(), getPassword());
            st = con.createStatement();
            
            String sql_var = "SELECT project_id FROM gros.project WHERE UPPER(name) = '" + projectName.toUpperCase().trim()+ "'";
            rs = st.executeQuery(sql_var);
 
            while (rs.next()) {
                idProject = rs.getInt("project_id");
            }
            
            
            
            con.close();
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
        return idProject;
        
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return the project
     */
    public String getProject() {
        return project;
    }

    /**
     * @param project the project to set
     */
    public void setProject(String project) {
        this.project = project;
    }

    /**
     * @return the projectID
     */
    public int getProjectID() {
        return projectID;
    }

    /**
     * @param projectID the projectID to set
     */
    public void setProjectID(int projectID) {
        this.projectID = projectID;
    }
    
}
