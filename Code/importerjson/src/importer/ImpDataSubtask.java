/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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