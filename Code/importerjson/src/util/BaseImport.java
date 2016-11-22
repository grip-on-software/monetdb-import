/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

/**
 *
 * @author Enrique
 */

import dao.DataSource;
import java.io.File;
import java.sql.Connection;
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
        File f = new File(System.getProperty("java.class.path"));
        File dir = f.getAbsoluteFile().getParentFile();
        path = dir.toString();
        //project = bundle.getString("project").trim(); 
        //projectID = this.getProjectId(project);
     
    }
    
    private int getProjectId(String projectName){
    
        int idProject = 0;
        PreparedStatement pstmt = null;
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        
        try{
            con = DataSource.getInstance().getConnection();
            st = con.createStatement();
            
            String sql_var = "SELECT project_id FROM gros.project WHERE UPPER(name) = '" + projectName.toUpperCase().trim()+ "'";
            rs = st.executeQuery(sql_var);
 
            while (rs.next()) {
                idProject = rs.getInt("project_id");
            }
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
