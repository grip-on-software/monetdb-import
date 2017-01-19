/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

/**
 *
 * @author Thomas
 */

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import util.BaseDb;
import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * THe class is created for managing your database connection and having only
 * one database connection at the time.
 * @author Thomas
 */
public class DataSource extends BaseDb {

    private static DataSource     datasource;
    private final ComboPooledDataSource cpds;

    private DataSource() throws IOException, SQLException, PropertyVetoException {
        //System.setProperty("com.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL", "WARNING");
        //System.setProperty("com.mchange.v2.log.MLog", "com.mchange.v2.log.FallbackMLog");
        
        cpds = new ComboPooledDataSource();
        cpds.setDriverClass("nl.cwi.monetdb.jdbc.MonetDriver"); //loads the jdbc driver
        cpds.setJdbcUrl(getUrl());
        cpds.setUser(getUser());
        cpds.setPassword(getPassword());
        
        cpds.setMaxPoolSize(50);
        
    }

    /**
     * Returns the database object for the application. In case a database 
     * object is not yet created, it will create the object. 
     * @return Instance of Database connection
     */
    public static DataSource getInstance() throws IOException, SQLException, PropertyVetoException {
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
     */
    public final Connection getConnection() throws SQLException {
        return this.cpds.getConnection();
    }

}