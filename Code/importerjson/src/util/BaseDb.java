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
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for tracking database connection parameters as well as some
 * import and logging configuration.
 * @author Leon Helwerda
 */
public class BaseDb {
    private final ResourceBundle bundle;
    private String url;
    private String user;
    private String password;
    private final String rootPath;
    private String path;
    private static final Logger LOGGER = Logger.getLogger("importer");
    
    public BaseDb() {
        try {
            bundle = ResourceBundle.getBundle("util.import");
        }
        catch (MissingResourceException ex) {
            throw new RuntimeException("An import.properties must have been created during compile time", ex);
        }
        url = getProperty("url");
        user = getProperty("user");
        password = getProperty("password");
        
        // Get system file path from Java jar location.
        File f = new File(System.getProperty("java.class.path"));
        File dir = f.getAbsoluteFile().getParentFile();
        rootPath = dir.toString() + "/";
        String relPath = getProperty("relPath");
        path = rootPath + relPath + "/";
    }
    
    /**
     * Retrieve a property from the system properties or the resource bundle
     * @param name Property name
     * @return Textual property value
     */
    protected final String getProperty(String name) {
        String property = System.getProperty("importer." + name, bundle.getString(name));
        return property.trim();
    }
    
    /**
     * Log an exception that occurs while performing importing tasks.
     * @param ex The exception that occurred
     */
    protected final void logException(Exception ex) {
        // Get the source method
        StackTraceElement source = Thread.currentThread().getStackTrace()[2];
        LOGGER.logp(Level.SEVERE, source.getClassName(), source.getMethodName(), "Exception", ex);

        if (ex instanceof SQLException) {
            SQLException prev = (SQLException)ex;
            SQLException next = prev.getNextException();
            while (next != null && !next.getMessage().equals(prev.getMessage())) {
                LOGGER.logp(Level.SEVERE, source.getClassName(), source.getMethodName(), "Earlier exception", next);
                prev = next;
                next = prev.getNextException();
            }
        }
    }
    
    /**
     * Retrieve the logger object of the importer.
     * @return The logger
     */
    protected final Logger getLogger() {
        return LOGGER;
    }
    
    /**
     * Retrieve the ResourceBundle of the importer.
     * @return The resource bundle
     */
    public final ResourceBundle getBundle() {
        return bundle;
    }
    
    /**
     * Retrieve the JDBC connection URL of the database.
     * @return The database URL
     */
    public final String getUrl() {
        return url;
    }

    /**
     * Change the JDBC connection URL of the database.
     * @param url The database URL to set
     */
    public final void setUrl(String url) {
        this.url = url;
    }

    /**
     * Retrieve the database username to connect with.
     * @return The username
     */
    public final String getUser() {
        return user;
    }

    /**
     * Change the database username to connect with.
     * @param user The username to set
     */
    public final void setUser(String user) {
        this.user = user;
    }

    /**
     * Retrieve the database password to connect with.
     * @return The password
     */
    public final String getPassword() {
        return password;
    }

    /**
     * Change the database password to connect with.
     * @param password The password to set
     */
    public final void setPassword(String password) {
        this.password = password;
    }
    
    /**
     * Get the root path where files with properties for the importer are found.
     * @return The root path
     */
    public final String getRootPath() {
        return rootPath;
    }

    /**
     * Get the path where the gathered data is stored for import.
     * @return The path where the gathered data is found
     */
    public final String getPath() {
        return path;
    }

    /**
     * Change the path where the gathered data is stored for import.
     * This does not change the root path of the importer properties.
     * @param path The path to set
     */
    public final void setPath(String path) {
        this.path = path;
    }
    
    /**
     * Set a string parameter to a prepared statement. If the parameter is null,
     * then this method performs the appropriate action to set the designated
     * parameter to NULL.
     * @param pstmt The prepared statement to set the parameter in
     * @param index The index of the parameter
     * @param value The parameter value, possibly null
     * @throws SQLException If a database access error occurred
     */
    protected void setString(PreparedStatement pstmt, int index, String value) throws SQLException {
        if (value != null) {
            pstmt.setString(index, value);
        }
        else {
            pstmt.setNull(index, java.sql.Types.VARCHAR);
        }
    }
    
    /**
     * Set an integer parameter to a prepared statement. If the parameter is null,
     * then this method performs the appropriate action to set the designated
     * parameter to NULL.
     * @param pstmt The prepared statement to set the parameter in
     * @param index The index of the parameter
     * @param value The parameter value, possibly null
     * @throws SQLException If a database access error occurred
     */
    protected void setInteger(PreparedStatement pstmt, int index, Integer value) throws SQLException {
        if (value != null) {
            pstmt.setInt(index, value);
        }
        else {
            pstmt.setNull(index, java.sql.Types.INTEGER);
        }
    }
    
    /**
     * Set a timestamp parameter to a prepared statement. If the parameter is null,
     * then this method performs the appropriate action to set the designated
     * parameter to NULL.
     * @param pstmt The prepared statement to set the parameter in
     * @param index The index of the parameter
     * @param date The parameter value, possibly null
     * @throws SQLException If a database access error occurred
     */
    protected void setTimestamp(PreparedStatement pstmt, int index, Timestamp date) throws SQLException {
        if (date != null) {
            pstmt.setTimestamp(index, date);
        }
        else {
            pstmt.setNull(index, java.sql.Types.TIMESTAMP);
        }
    }
    
    /**
     * Set a date parameter to a prepared statement. If the parameter is null,
     * then this method performs the appropriate action to set the designated
     * parameter to NULL.
     * @param pstmt The prepared statement to set the parameter in
     * @param index The index of the parameter
     * @param date The parameter value, possibly null
     * @throws SQLException If a database access error occurred
     */
    protected void setDate(PreparedStatement pstmt, int index, Date date) throws SQLException {
        if (date != null) {
            pstmt.setDate(index, date);
        }
        else {
            pstmt.setNull(index, java.sql.Types.DATE);
        }
    }
    
}
