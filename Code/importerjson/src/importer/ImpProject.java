/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import util.BaseImport;
import dao.ProjectDb;
import java.io.BufferedReader;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Enrique
 */
public class ImpProject extends BaseImport{
    
    BufferedReader br = null;
    JSONParser parser = new JSONParser();
    ProjectDb pDB;

    public int parser(String projectN){

        int project = 0;
         
        try {
            
            pDB = new ProjectDb();
            project = pDB.check_project(projectN);
            
            if(project == 0){

                pDB.insert_project(projectN);
                project = pDB.check_project(projectN);
                    
            }
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
        finally{
      
            
        }
        
        return project;
        
    }
        

}
    
