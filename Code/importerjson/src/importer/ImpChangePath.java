/**
 * VCS change path importer.
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
import dao.RepositoryDb;
import java.beans.PropertyVetoException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import util.BaseImport;
import util.BufferedJSONReader;

/**
 * Importer for the VCS changed paths.
 * @author Leon Helwerda
 */
public class ImpChangePath extends BaseImport {
    
    @Override
    public void parser() {
        int projectID = getProjectID();
        String sql = "insert into gros.change_path(repo_id,version_id,file,insertions,deletions,type,size) values (?,?,?,?,?,?,?);";
 
        try (
            RepositoryDb repoDb = new RepositoryDb();
            FileReader fr = new FileReader(getMainImportPath());
            BufferedJSONReader br = new BufferedJSONReader(fr);
            BatchedCheckStatement cstmt = new BatchedCheckStatement("gros.change_path", sql,
                    new String[]{"repo_id", "version_id", "file"},
                    new int[]{java.sql.Types.INTEGER, java.sql.Types.VARCHAR, java.sql.Types.VARCHAR}
            ) {
                @Override
                protected void addToBatch(Object[] values, Object data, PreparedStatement pstmt) throws SQLException, PropertyVetoException {
                    int repo_id = (int)values[0];
                    String version_id = (String)values[1];
                    String file = (String)values[2];
                    JSONObject jsonObject = (JSONObject) data;
                    String insertions = (String) jsonObject.get("insertions");
                    String deletions = (String) jsonObject.get("deletions");
                    String change_type = (String) jsonObject.get("change_type");
                    String size = (String) jsonObject.get("size");
                    int file_size = 0;
                    if (size != null) {
                        file_size = Integer.parseInt(size);
                    }

                    pstmt.setInt(1, repo_id);
                    pstmt.setString(2, version_id);
                    pstmt.setString(3, file);
                    pstmt.setInt(4, Integer.parseInt(insertions));
                    pstmt.setInt(5, Integer.parseInt(deletions));
                    pstmt.setString(6, change_type);
                    pstmt.setInt(7, file_size);

                    insertStmt.batch();
                }
            }
        ) {
            Object o;
            while ((o = br.readObject()) != null) {
                JSONObject jsonObject = (JSONObject) o;
                
                String version_id = (String) jsonObject.get("version_id");
                String repo_name = (String) jsonObject.get("repo_name");
                String file = (String) jsonObject.get("file");
                
                int repo_id = repoDb.check_repo(repo_name, projectID);
                
                if (repo_id == 0) { // if repo id does not exist, create repo with new id
                    repoDb.insert_repo(repo_name, projectID);
                    repo_id = repoDb.check_repo(repo_name, projectID); // set new id of repo
                }
                
                Object[] values = new Object[]{repo_id, version_id, file};
                cstmt.batch(values, o);
            }
            
            cstmt.execute();
        }
        catch (FileNotFoundException ex) {
            getLogger().log(Level.WARNING, "Cannot import {0}: {1}", new Object[]{getImportName(), ex.getMessage()});
        }
        catch (IOException | SQLException | PropertyVetoException | ParseException | NumberFormatException ex) {
            logException(ex);
        }
    }
    
    @Override
    public String getImportName() {
        return "version control system changed paths";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_change_path.json"};
    }

}
    

