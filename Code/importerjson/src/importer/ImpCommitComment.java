/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.NoteDb;
import dao.RepositoryDb;
import dao.SaltDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 *
 * @author leonhelwerda
 */
public class ImpCommitComment extends BaseImport {

    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
 
        try (
            RepositoryDb repoDb = new RepositoryDb();
            NoteDb noteDb = new NoteDb();
            FileReader fr = new FileReader(getPath()+getProjectName()+"/data_commit_comment.json")
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
                JSONObject jsonObject = (JSONObject) o;
                
                String repo_name = (String) jsonObject.get("repo_name");
                String version_id = (String) jsonObject.get("commit_id");
                String author = (String) jsonObject.get("author");
                String comment = (String) jsonObject.get("comment");
                String file = (String) jsonObject.get("file");
                String line_number = (String) jsonObject.get("line");
                String line_type = (String) jsonObject.get("line_type");
                String encrypted = (String) jsonObject.get("encrypted");
                
                int repo_id = repoDb.check_repo(repo_name);
                if (repo_id == 0) {
                    throw new Exception("Cannot determine repository: " + repo_name);
                }
                
                if (file.equals("0")) {
                    file = null;
                }
                Integer line = Integer.parseInt(line_number);
                if (line == 0) {
                    line = null;
                }
                if (line_type.equals("0")) {
                    line_type = null;
                }
                int encryption = SaltDb.Encryption.parseInt(encrypted);
                
                if (!noteDb.check_commit_note(repo_id, version_id, author, comment, file, line, line_type, encryption)) {
                    noteDb.insert_commit_note(repo_id, version_id, author, comment, file, line, line_type, encryption);
                }
            }
            
        }
        catch (FileNotFoundException ex) {
            System.out.println("Cannot import " + getImportName() + ": " + ex.getMessage());
        }
        catch (Exception ex) {
            logException(ex);
        }

    }

    @Override
    public String getImportName() {
        return "GitLab commit comments";
    }
    
}
