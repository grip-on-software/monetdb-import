/*
 * Copyright 2017-2018 ICTU
 * 
 * Importer of gathered software development metrics for MonetDB
 */
package dao;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An object that represents a precompiled SQL update statement that can be
 * batched to decrease communication overhead with the database. The updates are
 * batched and updates are automatically performed for memory management purposes.
 * @author Leon Helwerda
 */
public class BatchedStatement implements AutoCloseable {
    private Connection con = null;
    private PreparedStatement pstmt = null;
    private String query = "";
    /**
     * Current number of update statements in the batch
     */
    private Integer batchSize;
    private static final Integer MAX_BATCH_SIZE = 1000;
    
    /**
     * Create a batched statement object for the given SQL query.
     * @param sql The SQL update query to perform in batches
     */
    public BatchedStatement(String sql) {
        query = sql;
        batchSize = 0;
    }
    
    /**
     * Retrieve the connection that the batched statement uses.
     * @return Connection object
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public Connection getConnection() throws SQLException, PropertyVetoException {
        if (con == null) {
            con = DataSource.getInstance().getConnection();
        }
        return con;
    }

    /**
     * Retrieve the prepared statement.
     * Callers can perform the usual parameter filling on the prepared statement
     * to fill a batch record.
     * @return The PreparedStatement object that the batched statement reuses
     * @throws SQLException If a database access error occurs or the connection is closed
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public PreparedStatement getPreparedStatement() throws SQLException, PropertyVetoException {
        getConnection();
        if (pstmt == null) {
            pstmt = con.prepareStatement(query);
            batchSize = 0;
        }
        return pstmt;
    }
    
    /**
     * Add the current prepared statement parameters to the batch.
     * Use this method after the parameters of the prepared statement are filled
     * to register this record in the batch.
     * @throws SQLException If a database access error occurs, or the connection
     * is closed, or the prepared statement is closed
     */
    public void batch() throws SQLException {
        if (pstmt != null) {
            pstmt.addBatch();
            pstmt.clearParameters();
            batchSize++;
            if (batchSize >= MAX_BATCH_SIZE) {
                execute();
            }
        }
    }
    
    /**
     * Immediately perform all updates recorded in the current batch.
     * Use this method to ensure the updates are flushed, for example when
     * successfully ending an import update. Callers can also use this to
     * "downgrade" the batched statement to a prepared statement
     * that manages its own connection.
     * @throws SQLException If a database access error occurs, or the connection
     * is closed, or the prepared statement is closed
     */
    public void execute() throws SQLException {
        if (pstmt != null) {
            int[] counts = pstmt.executeBatch();
            Logger.getLogger("importer").log(Level.FINE, "Updates for {0}: {1}", new Object[]{query, Arrays.toString(counts)});
            pstmt.clearBatch();
            batchSize = 0;
        }
    }

    /**
     * Close the connections opened by the prepared statement and free resources.
     * This does not flush the current batch.
     * @throws SQLException If a database access error occurs
     */
    @Override
    public void close() throws SQLException {
        if (con != null) {
            con.close();
            con = null;
        }
        if (pstmt != null) {
            pstmt.close();
            pstmt = null;
        }
        batchSize = 0;
    }

    /**
     * Retrieve the maximum batch size.
     * @return The maximum size of batches for the batched statement
     */
    public int getMaxBatchSize() {
        return MAX_BATCH_SIZE;
    }
}
