/*
 * Copyright 2017-2018 ICTU
 * 
 * Importer of gathered software development metrics for MonetDB
 */
package dao;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import util.BaseDb;
import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * The class is created for managing your database connection and having only
 * one database connection at the time.
 * @author Thomas
 */
public class DataSource extends BaseDb {
    /**
     * Singleton
     */
    private static DataSource datasource;
    private final ComboPooledDataSource cpds;

    /**
     * Create the database connection manager.
     * @throws PropertyVetoException If the JDBC driver cannot be registered
     */
    private DataSource() throws PropertyVetoException {
        cpds = new ComboPooledDataSource();
        // Load the JDBC driver.
        cpds.setDriverClass(getProperty("driver"));
        cpds.setJdbcUrl(getUrl());
        cpds.setUser(getUser());
        cpds.setPassword(getPassword());
        
        cpds.setMaxPoolSize(50);
    }

    /**
     * Returns the database singleton object for the importer application.
     * In case a database object is not yet created, it will create the object. 
     * @return Instance of DataSource
     * @throws PropertyVetoException If the JDBC driver cannot be registered
     */
    public static DataSource getInstance() throws PropertyVetoException {
        if (datasource == null) {
            datasource = new DataSource();
            return datasource;
        } else {
            return datasource;
        }
    }

    /**
     * Returns the connection with the database. 
     * @return Database connection
     * @throws SQLException If a database access error occurs
     */
    public final Connection getConnection() throws SQLException {
        return this.cpds.getConnection();
    }

}