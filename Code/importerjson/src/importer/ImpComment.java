/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import util.BaseImport;
import dao.CommentDb;
import java.io.FileReader;
import java.sql.SQLException;
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
        CommentDb commentDb = new CommentDb();
 
        try {
            
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+getProjectName()+"/data_comments.json"));
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String comment = (String) jsonObject.get("comment");
                comment = comment.replace("'", "\\'"); 
                String created = (String) jsonObject.get("created_at");
                String issue_id = (String) jsonObject.get("issue_id");
                String id = (String) jsonObject.get("id");
                String author = (String) jsonObject.get("author");
            
                commentDb.insert_comment(issue_id, comment, author, created);
                
            }
            
            commentDb.close();
        }
            
        catch (SQLException e) {
            printSQLExceptionDetails(e);
        }
        catch (Exception e) {
            e.printStackTrace();
        }        
    }
        

}
    
