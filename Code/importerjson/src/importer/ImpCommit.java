/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.DeveloperDb;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 *
 * @author Thomas and Enrique
 */
public class ImpCommit extends BaseImport{
    
    public void parser(int projectID, String projectN){

        BufferedReader br = null;
        PreparedStatement pstmt = null;
        Connection con = null;
        Statement st = null;
        JSONParser parser = new JSONParser();
        ResultSet rs = null;
        DeveloperDb devDb = new DeveloperDb();
 
        try {
            
            Class.forName("nl.cwi.monetdb.jdbc.MonetDriver");
            con = DriverManager.getConnection(getUrl(), getUser(), getPassword());
            
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+projectN+"/data_commits.json"));
            
            for (Object o : a)
            {
                JSONObject jsonObject = (JSONObject) o;
                
                String commit_id = (String) jsonObject.get("commit_id");
                String commit_date = (String) jsonObject.get("commit_date");
                String sprint_id = (String) jsonObject.get("sprint_id");
                String developer = (String) jsonObject.get("developer");
                String message = (String) jsonObject.get("message");
                String size_of_commit = (String) jsonObject.get("size_of_commit");
                String insertions = (String) jsonObject.get("insertions");
                String deletions = (String) jsonObject.get("deletions");
                String number_of_files = (String) jsonObject.get("number_of_files");
                String number_of_lines = (String) jsonObject.get("number_of_lines");
                String type = (String) jsonObject.get("type");
                
                if ((sprint_id.trim()).equals("null") ){ // In case not in between dates of sprint
                    sprint_id = "0";
                }

                int developer_id = devDb.check_developer(developer);
                
                if (developer_id == 0) { // in case developer id does not exist, create developer with new id and _git behind name
                    devDb.insert_developer("None",developer);
                }
                
                developer_id = devDb.check_developer(developer);

	        //con.close();
                
                String sql = "insert into gros.commits values (?,?,?,?,?,?,?,?,?,?,?,?);";
                
                pstmt = con.prepareStatement(sql);
                
                pstmt.setString(1, commit_id);
                pstmt.setInt(2, projectID);

                Timestamp ts_created;              
                if (commit_date != null){
                    ts_created = Timestamp.valueOf(commit_date); 
                    pstmt.setTimestamp(3,ts_created);
                } else{
                    //ts_created = null;
                    pstmt.setNull(3, java.sql.Types.TIMESTAMP);
                }

                pstmt.setInt(4, Integer.parseInt(sprint_id));

                // Calculate developerid Int or String?
                pstmt.setInt(5, developer_id);
                message = this.addSlashes(message);
                String new_message = message.replace("'", "\\'");
                pstmt.setString(6, new_message);
                pstmt.setInt(7, Integer.parseInt(size_of_commit));
                pstmt.setInt(8, Integer.parseInt(insertions));
                pstmt.setInt(9, Integer.parseInt(deletions));
                pstmt.setInt(10, Integer.parseInt(number_of_files));
                pstmt.setInt(11, Integer.parseInt(number_of_lines));
                pstmt.setString(12, type);

                pstmt.executeUpdate();
            }
            
            //Used for creating Project if it didn't exist
            this.setProjectID(projectID);
                  
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    public static String addSlashes(String s) {
        s = s.replaceAll("\\\\", "\\\\\\\\");
        s = s.replaceAll("\\n", "\\\\n");
        s = s.replaceAll("\\r", "\\\\r");
        s = s.replaceAll("\\00", "\\\\0");
        s = s.replaceAll("'", "\\\\'");
    return s;
}
        

}
    
