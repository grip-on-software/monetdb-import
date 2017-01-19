/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.io.File;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 *
 * @author Leon Helwerda
 */
public class BaseDb {
    private String url;
    private String user;
    private String password;
    private String path;
    
    public BaseDb() {
        ResourceBundle bundle = ResourceBundle.getBundle("util.import");
        url = bundle.getString("url").trim();
        user = bundle.getString("user").trim();
        password = bundle.getString("password").trim();
        //path = bundle.getString("path");
        
        // Get system file path from Java jar location.
        File f = new File(System.getProperty("java.class.path"));
        File dir = f.getAbsoluteFile().getParentFile();
        path = dir.toString() + "/";    
    }
    
    public void printSQLExceptionDetails(SQLException e) {
        e.printStackTrace();
        SQLException prev = e;
        SQLException ex = e.getNextException();
        while (ex != null && !ex.getMessage().equals(prev.getMessage())) {
            ex.printStackTrace();
            prev = ex;
            ex = ex.getNextException();
        }
    }
    
    /**
     * @return the url
     */
    public final String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public final void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return the user
     */
    public final String getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public final void setUser(String user) {
        this.user = user;
    }

    /**
     * @return the password
     */
    public final String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public final void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the path
     */
    public final String getPath() {
        return path;
    }

    /**
     * @param path the path to set
     */
    public final void setPath(String path) {
        this.path = path;
    }
}
