/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import util.BaseImport;
import dao.CommentDb;
import java.io.FileReader;
import java.sql.Timestamp;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Enrique
 */
public class ImpComment extends BaseImport{
    
    @Override
    public void parser() {

        JSONParser parser = new JSONParser();
 
        try (
            CommentDb commentDb = new CommentDb();
            FileReader fr = new FileReader(getPath()+getProjectName()+"/data_comments.json")
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
      
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
        

}
    
