/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import util.BaseImport;
import dao.ProjectDb;

/**
 * Importer for projects.
 * @author Enrique
 */
public class ImpProject extends BaseImport {    

    @Override
    public void parser() {
        ProjectDb pDB;
        String projectN = getProjectName();

        int project = 0;
         
        try {
            pDB = new ProjectDb();
            project = pDB.check_project(projectN);
            
            if (project == 0) {
                pDB.insert_project(projectN);
                project = pDB.check_project(projectN);
                    
            }
        }
        catch (Exception ex) {
            logException(ex);
        }
        finally {
            this.setProjectID(project);
        }
        
    }

    @Override
    public String getImportName() {
        return "project";
    }
        

}
    
