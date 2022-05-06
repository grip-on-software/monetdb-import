/**
 * A reusable SQL statement that performs "insert or update if exists" logic.
 * 
 * Copyright 2017-2020 ICTU
 * Copyright 2017-2022 Leiden University
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
	/**
	 * A map of values of key fields to select an existing row, and data fields
	 * to replace existing values with.
	 */
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
