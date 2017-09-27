/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import util.BaseImport;
import dao.ProjectDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Level;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Importer for projects.
 * @author Enrique
 */
public class ImpProject extends BaseImport {    

    @Override
    public void parser() {
        String name = getProjectName();
        
        boolean has_metadata = false;
        String main_project = null;
        String github_team = null;
        String gitlab_group = null;
        String quality_name = null;
        String quality_display_name = null;
        Boolean is_support_team = null;
        JSONParser parser = new JSONParser();

        int project = 0;
         
        try (ProjectDb pDB = new ProjectDb()) {
            project = pDB.check_project(name);
            try (
                FileReader fr = new FileReader(getMainImportPath());
            ) {
                JSONObject jsonObject = (JSONObject) parser.parse(fr);
                
                main_project = (String) jsonObject.get("main_project");
                github_team = (String) jsonObject.get("github_team");
                gitlab_group = (String) jsonObject.get("gitlab_group");
                quality_name = (String) jsonObject.get("quality_name");
                quality_display_name = (String) jsonObject.get("quality_display_name");
                is_support_team = (Boolean) jsonObject.get("is_support_team");
                has_metadata = true;
            }
            catch (FileNotFoundException ex) {
                getLogger().log(Level.WARNING, "Cannot import {0} (only the JIRA key): {1}", new Object[]{getImportName(), ex.getMessage()});
            }
            catch (Exception ex) {
                logException(ex);
            }
            
            if (project == 0) {
                pDB.insert_project(name, main_project, github_team, gitlab_group, quality_name, quality_display_name, is_support_team);
                project = pDB.check_project(name);
            }
            else if (has_metadata) {
                pDB.update_project(project, main_project, github_team, gitlab_group, quality_name, quality_display_name, is_support_team);
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
        return "project metadata";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_project.json"};
    }

}
    
