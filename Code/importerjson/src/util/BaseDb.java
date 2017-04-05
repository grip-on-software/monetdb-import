/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.io.File;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Leon Helwerda
 */
public class BaseDb {
    private String url;
    private String user;
    private String password;
    private String rootPath;
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
        rootPath = dir.toString() + "/";
        String relPath = System.getProperty("importer.relPath", bundle.getString("relPath"));
        path = rootPath + relPath + "/";
    }
    
    /**
     * Log an exception that occurs while performing importing tasks.
     * @param ex The exception that occurred
     */
    protected final void logException(Exception ex) {
        // Get the source method
        StackTraceElement source = Thread.currentThread().getStackTrace()[2];
        Logger.getLogger("importer").logp(Level.SEVERE, source.getClassName(), source.getMethodName(), "Exception", ex);

        if (ex instanceof SQLException) {
            SQLException prev = (SQLException)ex;
            SQLException next = prev.getNextException();
            while (next != null && !next.getMessage().equals(prev.getMessage())) {
                Logger.getLogger("importer").logp(Level.SEVERE, source.getClassName(), source.getMethodName(), "Earlier exception", next);
                prev = next;
                next = prev.getNextException();
            }
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
     * Get the root path where files with properties for the importer are found.
     * @return the root path
     */
    public final String getRootPath() {
        return rootPath;
    }

    /**
     * Get the path where the gathered data is stored for import.
     * @return the path where the gathered data is found
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

    protected void setString(PreparedStatement pstmt, int index, String value) throws SQLException {
        if (value != null) {
            pstmt.setString(index, value);
        }
        else {
            pstmt.setNull(index, java.sql.Types.VARCHAR);
        }
    }
    
    protected void setInteger(PreparedStatement pstmt, int index, Integer value) throws SQLException {
        if (value != null) {
            pstmt.setInt(index, value);
        }
        else {
            pstmt.setNull(index, java.sql.Types.INTEGER);
        }
    }
    
    protected void setTimestamp(PreparedStatement pstmt, int index, Timestamp date) throws SQLException {
        if (date != null) {
            pstmt.setTimestamp(index, date);
        }
        else {
            pstmt.setNull(index, java.sql.Types.TIMESTAMP);
        }
    }
    
    protected void setDate(PreparedStatement pstmt, int index, Date date) throws SQLException {
        if (date != null) {
            pstmt.setDate(index, date);
        }
        else {
            pstmt.setNull(index, java.sql.Types.DATE);
        }
    }
    
}
