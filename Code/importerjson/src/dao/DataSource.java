/**
 * Database connection management.
 * 
 * Copyright 2017-2020 ICTU
 * Copyright 2017-2022 Leiden University
 * Copyright 2017-2024 Leon Helwerda
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
package dao;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import util.BaseDb;
import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * Database connection management. This class ensures that there is only one
 * database connection at the same time.
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
     * Resets the singleton object.
     */
    public static void reset() {
        datasource.cpds.close();
        datasource = null;
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
