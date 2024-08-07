/**
 * VCS review system merge request note importer.
 * 
 * Copyright 2017-2020 ICTU
 * Copyright 2017-2022 Leiden University
 * Copyright 2017-2024 Leon Helwerda
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
 * Importer for GitLab merge request notes or Team Foundation Server non-commit comments.
 * @author Leon Helwerda
 */
public class ImpMergeRequestNote extends BaseImport {

    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        int project_id = this.getProjectID();
 
        try (
            RepositoryDb repoDb = new RepositoryDb();
            DeveloperDb devDb = new DeveloperDb();
            NoteDb noteDb = new NoteDb();
            FileReader fr = new FileReader(getMainImportPath())
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
                JSONObject jsonObject = (JSONObject) o;
                
                String repo_name = (String) jsonObject.get("repo_name");
                String merge_request_id = (String) jsonObject.get("merge_request_id");
                String thread = (String) jsonObject.get("thread_id");
                String note = (String) jsonObject.get("note_id");
                String parent = (String) jsonObject.get("parent_id");
                String author = (String) jsonObject.get("author");
                String author_username = (String) jsonObject.get("author_username");
                String comment = (String) jsonObject.get("comment");
                String created_at = (String) jsonObject.get("created_at");
                String updated_at = (String) jsonObject.get("updated_at");
                String encrypted = (String) jsonObject.get("encrypted");
                
                int encryption = SaltDb.Encryption.parseInt(encrypted);
                int repo_id = repoDb.check_repo(repo_name, project_id);
                if (repo_id == 0) {
                    getLogger().log(Level.WARNING, "Cannot determine repository in {0}: {1}", new Object[]{getMainImportPath(), repo_name});
                    continue;
                }
                int request_id = Integer.parseInt(merge_request_id);
                int thread_id = Integer.parseInt(thread);
                int note_id = Integer.parseInt(note);
                int parent_id = Integer.parseInt(parent);
                
                Developer dev = new Developer(author_username, author, null);
                int dev_id = devDb.update_vcs_developer(project_id, dev, encryption);
                
                if (!noteDb.check_request_note(repo_id, request_id, thread_id, note_id)) {
                    Timestamp created_date = Timestamp.valueOf(created_at);
                    Timestamp updated_date;
                    if (updated_at == null || updated_at.equals("0")) {
                        updated_date = null;
                    }
                    else {
                        updated_date = Timestamp.valueOf(updated_at);
                    }
                
                    noteDb.insert_request_note(repo_id, request_id, thread_id, note_id, parent_id, dev_id, comment, created_date, updated_date);
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
        return "GitLab/TFS merge request notes";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_merge_request_note.json"};
    }
    
}
