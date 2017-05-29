/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.BatchedStatement;
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
        String sql = "insert into gros.change_path values (?,?,?,?,?,?);";
 
        try (
            RepositoryDb repoDb = new RepositoryDb();
            FileReader fr = new FileReader(getPath()+getProjectName()+"/data_change_path.json");
            BufferedJSONReader br = new BufferedJSONReader(fr);
            BatchedStatement bstmt = new BatchedStatement(sql)
        ) {
                
            PreparedStatement pstmt = bstmt.getPreparedStatement();
            
            Object o;
            while ((o = br.readObject()) != null) {
                JSONObject jsonObject = (JSONObject) o;
                
                String version_id = (String) jsonObject.get("version_id");
                String repo_name = (String) jsonObject.get("repo_name");
                String file = (String) jsonObject.get("file");
                String insertions = (String) jsonObject.get("insertions");
                String deletions = (String) jsonObject.get("deletions");
                String change_type = (String) jsonObject.get("change_type");
                
                int repo_id = repoDb.check_repo(repo_name);
                
                if (repo_id == 0) { // if repo id does not exist, create repo with new id
                    repoDb.insert_repo(repo_name);
                    repo_id = repoDb.check_repo(repo_name); // set new id of repo
                }
                
                pstmt.setInt(1, repo_id);
                pstmt.setString(2, version_id);
                pstmt.setString(3, file);
                pstmt.setInt(4, Integer.parseInt(insertions));
                pstmt.setInt(5, Integer.parseInt(deletions));
                pstmt.setString(6, change_type);

                bstmt.batch();
            }
            
            bstmt.execute();
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
        return "VCS changed paths";
    }

}
    

