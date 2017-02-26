/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.DataSource;
import util.BaseImport;
import dao.RelationshipTypeDb;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Enrique
 */
public class ImpDataRelationshipType extends BaseImport{
    
    @Override
    public void parser(){

        PreparedStatement pstmt = null;
        Connection con = null;
        JSONParser parser = new JSONParser();
        RelationshipTypeDb relTypeDB;
        int rel_id = 0;
 
        try (FileReader fr = new FileReader(getPath()+getProjectName()+"/data_relationshiptype.json")) {
            con = DataSource.getInstance().getConnection();
            
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String id = (String) jsonObject.get("id");
                String name = (String) jsonObject.get("name");
                
                relTypeDB = new RelationshipTypeDb();
                rel_id = relTypeDB.check_relType(Integer.parseInt(id));
            
                if(rel_id == 0){

                    relTypeDB.insert_relType(name);
                    
                }
            }
            
        }
            
        catch (SQLException e) {
            printSQLExceptionDetails(e);
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {e.printStackTrace();}
            if (con != null) try { con.close(); } catch (SQLException e) {e.printStackTrace();}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {e.printStackTrace();}
        }
        
    }
        

}
    
