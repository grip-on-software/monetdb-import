/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An object that represents an "insert if not exists" update statement,
 * where both the selection check and insertion update queries are batched to
 * decrease communication overhead with the database. This is useful for
 * database management systems that do not contain such a compound statement,
 * and in cases where large tables may take a lot of overhead for checking
 * existence before executing a batch. This ensures that the insert batch itself
 * can still add new rows even if existing rows appear in the input data.
 * @author Leon Helwerda
 */
public abstract class BatchedCheckStatement implements AutoCloseable {
    /**
     * The batched statement that is used for inserts of new rows.
     */
    protected final BatchedStatement insertStmt;
    /**
     * The fully qualified table name where the checks are applied to (with scheme).
     */
    protected final String table;
    /**
     * An array of key names to request for checking unique and existing rows.
     */
    protected final String[] keys;
    /**
     * The field types of the keys that are retrieved.
     */
    protected final int[] types;
    private int batchSize;
    protected Map<List<Object>, Object> checkValues;
    
    private static int[] makeDefaultTypes(int length) {
        int[] types = new int[length];
        Arrays.fill(types, java.sql.Types.INTEGER);
        return types;
    }
    
    /**
     * Create a batched checked statement. All the keys are integers.
     * @param table The fully qualified table name where the checks are applied to (with scheme).
     * @param insertSql The SQL statement for inserting a row.
     * @param keys An array of key names to request for checking unique and existing rows.
     */
    public BatchedCheckStatement(String table, String insertSql, String[] keys) {
        this(table, insertSql, keys, makeDefaultTypes(keys.length));
    }
    
    /**
     * Create a batched checked statement.
     * @param table The fully qualified table name where the checks are applied to (with scheme).
     * @param insertSql The SQL statement for inserting a row.
     * @param keys An array of key names to request for checking unique and existing rows.
     * @param types An array of SQL types of the keys.
     */
    public BatchedCheckStatement(String table, String insertSql, String[] keys, int[] types) {
        this.insertStmt = new BatchedStatement(insertSql);
        this.table = table;
        this.keys = Arrays.copyOf(keys, keys.length);
        this.types = types;
        batchSize = this.insertStmt.getMaxBatchSize();
        checkValues = new HashMap<>();
    }
    
    /**
     * Add a tuple of keys that needs to be checked for existence.
     * @param values The tuple of key values to check against.
     * @param data Provider of data that can be processed after the existence
     * check has been performed in order to add parameters to the insert query. 
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public void batch(Object[] values, Object data) throws SQLException, PropertyVetoException {
        checkValues.put(Collections.unmodifiableList(Arrays.asList(values)), data);
        if (checkValues.size() >= batchSize) {
            execute();
        }
    }
    
    /**
     * Add a tuple with its associated data to the insert batch.
     * @param values The tuple of key values that belong to the data and do not
     * exist in the table.
     * @param data Provider of data that are processed in order to add
     * parameters to the insert query.
     * @param pstmt Prepared statement of the insert batch query to add
     * parameters to.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    protected abstract void addToBatch(Object[] values, Object data, PreparedStatement pstmt) throws SQLException, PropertyVetoException;
    
    private String buildQuery() {
        List<String> keyClauses = new ArrayList<>(keys.length);
        for (String key : keys) {
            keyClauses.add(key + " = ?");
        }
        String clause = "(" + String.join(" and ", keyClauses) + ")";
        List<String> clauses = Collections.nCopies(checkValues.size(), clause);
        
        return "select " + String.join(", ", keys) + " from " + table + " where (" + String.join(" or ", clauses) + ")";
    }
    
    /**
     * Perform the batched existence and insertion queries.
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     * @return Whether any value was inserted
     */
    public boolean execute() throws SQLException, PropertyVetoException {
        if (checkValues.isEmpty()) {
            return false;
        }
        
        String selectSql = buildQuery();
        
        Connection con = insertStmt.getConnection();
        try (PreparedStatement selectStmt = con.prepareStatement(selectSql)) {
            int index = 1;
            for (List<Object> values : checkValues.keySet()) {
                for (int i = 0; i < types.length; i++) {
                    selectStmt.setObject(index, values.get(i), types[i]);
                    index++;
                }
            }
        
            // Determine which key tuples already exist in the table and remove the found ones from the checkValues.
            try (ResultSet rs = selectStmt.executeQuery()) {
                while (rs.next()) {
                    List<Object> foundValues = new ArrayList<>(keys.length);
                    for (String key : keys) {
                        foundValues.add(rs.getObject(key));
                    }
                    if (!checkValues.containsKey(foundValues)) {
                        throw new SQLException("Received result key tuple that is not in the check batch: " + Arrays.toString(foundValues.toArray()));
                    }
                    markExisting(foundValues);
                }
            }
        }
        
        // Insert the remaining values into the batch
        PreparedStatement pstmt = insertStmt.getPreparedStatement();
        Logger.getLogger("importer").log(Level.FINE, "Remaining key tuples to insert: {0}", checkValues.size());
        
        for (HashMap.Entry<List<Object>, Object> entry : checkValues.entrySet()) {
            List<Object> values = entry.getKey();
            Object data = entry.getValue();
            addToBatch(values.toArray(), data, pstmt);
        }
        
        boolean hasInserts = !checkValues.isEmpty();
        checkValues.clear();
        insertStmt.execute();
        return hasInserts;
    }

    protected void markExisting(List<Object> foundValues) {
        checkValues.remove(foundValues);
    }

    /**
     * Retrieve the connection that the batched statement uses.
     * @return Connection object
     * @throws SQLException If a database access error occurs
     * @throws PropertyVetoException If the database connection cannot be configured
     */
    public Connection getConnection() throws SQLException, PropertyVetoException {
        return insertStmt.getConnection();
    }
    
    /**
     * Retrieve the batch size.
     * @return Size of the selection query batch
     */
    public final int getBatchSize() {
        return batchSize;
    }

    /**
     * Alter the batch size.
     * @param batchSize Size of the selection query batch
     */
    public final void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * Close the connections opened by the batched statement and free resources.
     * This does not execute the current batch.
     * @throws SQLException If a database access error occurs
     */
    @Override
    public void close() throws SQLException {
        insertStmt.close();
        checkValues.clear();
    }
    
}
