/**
 * Database access management.
 * 
 * Copyright 2017-2020 ICTU
 * Copyright 2017-2022 Leiden University
 * Copyright 2017-2023 Leon Helwerda
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package util;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
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
    private class ConfigurationException extends RuntimeException {
        public ConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    private final ResourceBundle bundle;
    private String url;
    private String user;
    private String password;
    private final Path rootPath;
    private Path path;
    private static final Logger LOGGER = Logger.getLogger("importer");
    private boolean hasExceptions = false;
    
    public BaseDb() {
        try {
            bundle = ResourceBundle.getBundle("util.import");
        }
        catch (MissingResourceException ex) {
            throw new ConfigurationException("An import.properties must have been created during compile time", ex);
        }
        url = getProperty("url");
        user = getProperty("user");
        password = getProperty("password");
        
        // Get system file path from Java jar location.
        File f = new File(System.getProperty("user.dir"));
        File dir = f.getAbsoluteFile();
        rootPath = dir.toPath();
        String relPath = getProperty("relPath");
        File absPath = new File(dir, relPath);
        path = absPath.toPath();
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
        hasExceptions = true;
        
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
    
    public final boolean hasExceptions() {
        return hasExceptions;
    }

    /**
     * Close a prepared statement object.
     * @param pstmt The prepared statement
     * @throws SQLException If a database access error occurs
     */
    protected void closeStatement(PreparedStatement pstmt) throws SQLException {
        if (pstmt != null) {
            pstmt.close();
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
    public final Path getRootPath() {
        return rootPath;
    }

    /**
     * Get the path where the gathered data is stored for import.
     * @return The path where the gathered data is found
     */
    public final Path getPath() {
        return path;
    }
    
    /**
     * Get the path where the gathered data for a specific project is stored
     * for import.
     * @param project The project identifier
     * @return The path where the project's gathered data is found
     */
    public final File getProjectPath(String project) {
        return new File(path.toFile(), project);
    }

    /**
     * Change the path where the gathered data is stored for import.
     * This does not change the root path of the importer properties.
     * @param path The path to set
     */
    public final void setPath(String path) {
        File dir = new File(path);
        this.path = dir.toPath();
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
     * Set a string parameter to a prepared statement. If the parameter is null,
     * then this method performs the appropriate action to set the designated
     * parameter to NULL. Otherwise, we ensure that the length of the string is
     * limited to the field size.
     * @param pstmt The prepared statement to set the parameter in
     * @param index The index of the parameter
     * @param value The parameter value, possibly null
     * @param limit The size of the field to limit the string length to
     * @throws SQLException If a database access error occurred
     */
    protected void setString(PreparedStatement pstmt, int index, String value, int limit) throws SQLException {
        if (value != null) {
            pstmt.setString(index, value.length() > limit ? value.substring(0, limit) : value);
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
     * Set an integer parameter to a prepared statement parsed from a string.
     * If the parameter is null, then this method performs the appropriate
     * action to set the designated parameter to NULL.
     * @param pstmt The prepared statement to set the parameter in
     * @param index The index of the parameter
     * @param value The parameter value to parse, possibly null
     * @throws SQLException If a database access error occurred
     */
    protected void setInteger(PreparedStatement pstmt, int index, String value) throws SQLException {
        if (value != null) {
            int number = Integer.parseInt(value);
            pstmt.setInt(index, number);
        } else {
            pstmt.setNull(index, java.sql.Types.INTEGER);
        }
    }
    
    /**
     * Set a floating point parameter to a prepared statement. If the parameter
     * is null, then this method performs the appropriate action to set the
     * designated parameter to NULL.
     * @param pstmt The prepared statement to set the parameter in
     * @param index The index of the parameter
     * @param value The parameter value, possibly null
     * @throws SQLException If a database access error occurred
     */
    protected void setFloat(PreparedStatement pstmt, int index, Float value) throws SQLException {
        if (value != null) {
            pstmt.setFloat(index, value);
        }
        else {
            pstmt.setNull(index, java.sql.Types.FLOAT);
        }
    }
    
    /**
     * Set a decimal value parameter to a prepared statement parsed from a string.
     * If the parameter is null, then this method performs the appropriate
     * action to set the designated parameter to NULL.
     * @param pstmt The prepared statement to set the parameter in
     * @param index The index of the parameter
     * @param value The parameter value to parse, possibly null
     * @param maximum The maximum value that the value may have in the field
     * @throws SQLException If a database access error occurred
     */
    protected void setDouble(PreparedStatement pstmt, int index, String value, BigDecimal maximum) throws SQLException {
        if (value != null) {
            BigDecimal points = BigDecimal.valueOf(Double.parseDouble(value));
            pstmt.setBigDecimal(index, points.min(maximum));
        }
        else {
            pstmt.setNull(index, java.sql.Types.NUMERIC);
        }
    }
    
    /**
     * Set a boolean parameter to a prepared statement. If the parameter is null,
     * then this method performs the appropriate action to set the designated
     * parameter to NULL.
     * @param pstmt The prepared statement to set the parameter in
     * @param index The index of the parameter
     * @param value The parameter value, possibly null
     * @throws SQLException If a database access error occurred
     */
    protected void setBoolean(PreparedStatement pstmt, int index, Boolean value) throws SQLException {
        if (value != null) {
            pstmt.setBoolean(index, value);
        }
        else {
            pstmt.setNull(index, java.sql.Types.BOOLEAN);
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
     * Set a timestamp parameter to a prepared statement parsed from a string.
     * If the parameter is null, then this method performs the appropriate
     * action to set the designated parameter to NULL.
     * @param pstmt The prepared statement to set the parameter in
     * @param index The index of the parameter
     * @param value The parameter value to parse, possibly null
     * @throws SQLException If a database access error occurred
     */
    protected void setTimestamp(PreparedStatement pstmt, int index, String value) throws SQLException {
        if (value != null){
            Timestamp date = Timestamp.valueOf(value);
            pstmt.setTimestamp(index, date);
        } else{
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
    
    /**
     * Set a date parameter to a prepared statement parsed from a string.
     * If the parameter is null, then this method performs the appropriate
     * action to set the designated parameter to NULL.
     * @param pstmt The prepared statement to set the parameter in
     * @param index The index of the parameter
     * @param value The parameter value to parse, possibly null
     * @throws SQLException If a database access error occurred
     */
    protected void setDate(PreparedStatement pstmt, int index, String value) throws SQLException {
        if (value != null){
            Date date = Date.valueOf(value);
            pstmt.setDate(index, date);
        } else{
            pstmt.setNull(index, java.sql.Types.DATE);
        }
    }

}
