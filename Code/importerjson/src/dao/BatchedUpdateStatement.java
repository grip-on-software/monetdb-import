/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import java.beans.PropertyVetoException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An object that represents an "insert or update if exists" update statement,
 * where the selection check and insertion and update queries are batched to
 * decrease communication overhead with the database.
 * @author Leon Helwerda
 */
public abstract class BatchedUpdateStatement extends BatchedCheckStatement {
    /**
     * The batched statement that is used for updates of existing rows.
     */
    protected final BatchedStatement updateStmt;
    protected Map<List<Object>, Object> updateValues = new HashMap<>();

    
    public BatchedUpdateStatement(String table, String insertSql, String updateSql, String[] keys) {
        super(table, insertSql, keys);
        this.updateStmt = new BatchedStatement(updateSql);
    }

    protected abstract void addToUpdateBatch(Object[] values, Object data, PreparedStatement pstmt) throws SQLException, PropertyVetoException;    
    
    @Override
    public boolean execute() throws SQLException, PropertyVetoException {
        boolean hasInserts = super.execute();
        boolean hasUpdates = !updateValues.isEmpty();
        
        // Insert the update values into the batch
        PreparedStatement pstmt = updateStmt.getPreparedStatement();
        Logger.getLogger("importer").log(Level.FINE, "Key tuples to update: {0}", updateValues.size());
        
        for (HashMap.Entry<List<Object>, Object> entry : updateValues.entrySet()) {
            List<Object> values = entry.getKey();
            Object data = entry.getValue();
            addToUpdateBatch(values.toArray(), data, pstmt);
        }

        updateValues.clear();
        updateStmt.execute();
        return hasInserts || hasUpdates;
    }
    
    @Override
    protected void markExisting(List<Object> foundValues) {
        updateValues.put(foundValues, checkValues.get(foundValues));
        super.markExisting(foundValues);
    }
    
    @Override
    public void close() throws SQLException {
        super.close();
        updateStmt.close();
        updateValues.clear();
    }
}
