/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import util.BaseImport;
import dao.CommentDb;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Enrique
 */
public class ImpComment extends BaseImport{
    
    public void parser(String projectN){

        JSONParser parser = new JSONParser();
        CommentDb commentDb;
 
        try {
            
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+projectN+"/data_comments.json"));
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String comment = (String) jsonObject.get("comment");
                String created = (String) jsonObject.get("created_at");
                String issue_id = (String) jsonObject.get("issue_id");
                String id = (String) jsonObject.get("id");
                String author = (String) jsonObject.get("author");
            
                commentDb = new CommentDb();
                commentDb.insert_comment(issue_id, comment, author, created);
                
            }
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
        

}
    
