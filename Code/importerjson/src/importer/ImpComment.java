/**
 * JIRA comment importer.
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

import util.BaseImport;
import dao.CommentDb;
import java.io.FileReader;
import java.sql.Timestamp;
import org.json.simple.JSONObject;
import util.BufferedJSONReader;

/**
 * Importer for the JIRA comments.
 * @author Enrique
 */
public class ImpComment extends BaseImport {
    
    @Override
    public void parser() {
        try (
            CommentDb commentDb = new CommentDb();
            FileReader fr = new FileReader(getMainImportPath());
            BufferedJSONReader br = new BufferedJSONReader(fr)
        ) {
            Object o;
            while ((o = br.readObject()) != null) {
                JSONObject jsonObject = (JSONObject) o;
                
                String comment = (String) jsonObject.get("comment");
                String created = (String) jsonObject.get("created_at");
                String issue_id = (String) jsonObject.get("issue_id");
                String id = (String) jsonObject.get("id");
                String author = (String) jsonObject.get("author");
                String updater = (String) jsonObject.get("updater");
                String updated_at = (String) jsonObject.get("updated_at");
                
                int comment_id = Integer.parseInt(id);
                Timestamp date = Timestamp.valueOf(created);
                Timestamp updated_date = Timestamp.valueOf(updated_at);
                
                CommentDb.CheckResult result = commentDb.check_comment(comment_id, updater, author, date, updater, updated_date);
                if (result == CommentDb.CheckResult.MISSING) {
                    commentDb.insert_comment(comment_id, Integer.parseInt(issue_id), comment, author, date, updater, updated_date);
                }
                else if (result == CommentDb.CheckResult.DIFFERS) {
                    commentDb.update_comment(comment_id, comment, author, date, updater, updated_date);
                }
                
            }
        }
        catch (Exception ex) {
            logException(ex);
        }        
    }

    @Override
    public String getImportName() {
        return "JIRA comments";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_comments.json"};
    }
        

}
    
