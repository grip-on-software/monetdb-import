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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import util.BaseImport;

/**
 *
 * @author leonhelwerda
 */
public class ImpChangePath extends BaseImport{
    
    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        int projectID = getProjectID();
        String sql = "insert into gros.change_path values (?,?,?,?,?);";
 
        try (
            RepositoryDb repoDb = new RepositoryDb();
            FileReader fr = new FileReader(getPath()+getProjectName()+"/data_change_path.json");
            BatchedStatement bstmt = new BatchedStatement(sql)
        ) {
                
            PreparedStatement pstmt = bstmt.getPreparedStatement();
            
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
                JSONObject jsonObject = (JSONObject) o;
                
                String version_id = (String) jsonObject.get("version_id");
                String repo_name = (String) jsonObject.get("repo_name");
                String file = (String) jsonObject.get("file");
                String insertions = (String) jsonObject.get("insertions");
                String deletions = (String) jsonObject.get("deletions");
                
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

                bstmt.batch();
            }
            
            bstmt.execute();
        }
        catch (FileNotFoundException ex) {
            System.out.println("Cannot import " + getImportName() + ": " + ex.getMessage());
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
    

