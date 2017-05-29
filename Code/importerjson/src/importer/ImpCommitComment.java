/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.DeveloperDb;
import dao.DeveloperDb.Developer;
import dao.NoteDb;
import dao.RepositoryDb;
import dao.SaltDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Timestamp;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 * Importer for GitLab commit comments or Team Foundation Server commit review comments.
 * @author Leon Helwerda
 */
public class ImpCommitComment extends BaseImport {

    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        int project_id = this.getProjectID();
 
        try (
            RepositoryDb repoDb = new RepositoryDb();
            DeveloperDb devDb = new DeveloperDb();
            NoteDb noteDb = new NoteDb();
            FileReader fr = new FileReader(getPath()+getProjectName()+"/data_commit_comment.json")
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
                JSONObject jsonObject = (JSONObject) o;
                
                String repo_name = (String) jsonObject.get("repo_name");
                String version_id = (String) jsonObject.get("commit_id");
                String merge_request_id = (String) jsonObject.get("merge_request_id");
                String thread = (String) jsonObject.get("thread_id");
                String note = (String) jsonObject.get("note_id");
                String parent = (String) jsonObject.get("parent_id");
                String author = (String) jsonObject.get("author");
                String author_username = (String) jsonObject.get("author_username");
                String comment = (String) jsonObject.get("comment");
                String file = (String) jsonObject.get("file");
                String line_number = (String) jsonObject.get("line");
                String end_line_number = (String) jsonObject.get("end_line");
                String line_type = (String) jsonObject.get("line_type");
                String created_at = (String) jsonObject.get("created_date");
                String updated_at = (String) jsonObject.get("updated_date");
                String encrypted = (String) jsonObject.get("encrypted");
                
                int encryption = SaltDb.Encryption.parseInt(encrypted);
                int repo_id = repoDb.check_repo(repo_name);
                if (repo_id == 0) {
                    throw new Exception("Cannot determine repository: " + repo_name);
                }
                
                Developer dev = new Developer(author_username, author, null);
                int dev_id = devDb.check_project_developer(project_id, dev, encryption);
                
                int request_id = Integer.parseInt(merge_request_id);
                int thread_id = Integer.parseInt(thread);
                int note_id = Integer.parseInt(note);
                int parent_id = Integer.parseInt(parent);
                
                if (file.equals("0")) {
                    file = null;
                }
                Integer line = Integer.parseInt(line_number);
                if (line == 0) {
                    line = null;
                }
                Integer end_line;
                if (end_line_number == null || end_line_number.equals("0")) {
                    end_line = null;
                }
                else {
                    end_line = Integer.parseInt(end_line_number);
                }
                if (line_type.equals("0")) {
                    line_type = null;
                }
                
                Timestamp created_date;
                if (created_at == null || created_at.equals("0")) {
                    created_date = null;
                }
                else {
                    created_date = Timestamp.valueOf(created_at);
                }
                
                Timestamp updated_date;
                if (updated_at == null || updated_at.equals("0")) {
                    updated_date = null;
                }
                else {
                    updated_date = Timestamp.valueOf(updated_at);
                }
                
                Timestamp current_date = noteDb.check_commit_note(repo_id, version_id, request_id, thread_id, note_id, parent_id, dev_id, comment, file, line, end_line, line_type, created_date);
                if (current_date == null || (updated_date != null && !updated_date.equals(current_date))) {
                    noteDb.insert_commit_note(repo_id, version_id, request_id, thread_id, note_id, parent_id, dev_id, comment, file, line, end_line, line_type, created_date, updated_date);
                }
            }
            
        }
        catch (FileNotFoundException ex) {
            getLogger().log(Level.WARNING, "Cannot import {0}: {1}", new Object[]{getImportName(), ex.getMessage()});
        }
        catch (Exception ex) {
            logException(ex);
        }

    }

    @Override
    public String getImportName() {
        return "GitLab/TFS commit comments";
    }
    
}
