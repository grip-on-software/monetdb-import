/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importerjson;

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

    public void parser(){

        int project = 0;
         
        try {
            
            pDB = new ProjectDb();
            project = pDB.check_project(this.getProject());
            
            if(project == 0){

                pDB.insert_project(this.getProject());
                project = pDB.check_project(this.getProject());
                this.setProjectID(project);
                    
            }
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
        finally{
      
            
        }
        
    }
        

}
    
