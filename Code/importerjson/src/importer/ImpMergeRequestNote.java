/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.NoteDb;
import dao.RepositoryDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Timestamp;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 *
 * @author leonhelwerda
 */
public class ImpMergeRequestNote extends BaseImport {

    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
 
        try (
            RepositoryDb repoDb = new RepositoryDb();
            NoteDb noteDb = new NoteDb();
            FileReader fr = new FileReader(getPath()+getProjectName()+"/data_merge_request_note.json")
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
                JSONObject jsonObject = (JSONObject) o;
                
                String repo_name = (String) jsonObject.get("repo_name");
                String merge_request_id = (String) jsonObject.get("merge_request_id");
                String note = (String) jsonObject.get("note_id");
                String author = (String) jsonObject.get("author");
                String comment = (String) jsonObject.get("comment");
                String created_at = (String) jsonObject.get("created_at");
                
                int repo_id = repoDb.check_repo(repo_name);
                if (repo_id == 0) {
                    throw new Exception("Cannot determine repository: " + repo_name);
                }
                int request_id = Integer.parseInt(merge_request_id);
                int note_id = Integer.parseInt(note);
                
                if (!noteDb.check_request_note(repo_id, request_id, note_id)) {
                    Timestamp created_date = Timestamp.valueOf(created_at);
                
                    noteDb.insert_request_note(repo_id, request_id, note_id, author, comment, created_date);
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
        return "GitLab merge request notes";
    }
    
}
