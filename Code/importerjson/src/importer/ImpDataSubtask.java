/**
 * JIRA subtask importer.
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
package importer;

import dao.BatchedCheckStatement;
import java.beans.PropertyVetoException;
import util.BaseImport;
import java.io.FileReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.json.simple.JSONObject;
import util.BufferedJSONReader;

/**
 * Importer for JIRA issue subtask relations.
 * @author Enrique
 */
public class ImpDataSubtask extends BaseImport {
    
    @Override
    public void parser() {
        String sql = "insert into gros.subtask values (?,?);";
 
        try (
            FileReader fr = new FileReader(getMainImportPath());
            BufferedJSONReader br = new BufferedJSONReader(fr);
            BatchedCheckStatement cstmt = new BatchedCheckStatement("gros.subtask", sql,
                    new String[]{"id_parent", "id_subtask"},
                    new int[]{java.sql.Types.INTEGER, java.sql.Types.INTEGER}
            ) {
                @Override
                protected void addToBatch(Object[] values, Object data, PreparedStatement pstmt) throws SQLException, PropertyVetoException {
                    int id_parent = (int)values[0];
                    int id_subtask = (int)values[1];
                    pstmt.setInt(1, id_parent);
                    pstmt.setInt(2, id_subtask);

                    insertStmt.batch();
                }
            }
        ) {
            Object o;
            while ((o = br.readObject()) != null) {
                JSONObject jsonObject = (JSONObject) o;
                                
                String from_id = (String) jsonObject.get("from_id");
                String to_id = (String) jsonObject.get("to_id");
                
                Object[] values = new Object[]{Integer.parseInt(from_id), Integer.parseInt(to_id)};
                cstmt.batch(values, o);
            }
            
            cstmt.execute();
        }
        catch (Exception ex) {
            logException(ex);
        }
        
    }

    @Override
    public String getImportName() {
        return "JIRA issue subtasks";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_subtasks.json"};
    }
        

}
