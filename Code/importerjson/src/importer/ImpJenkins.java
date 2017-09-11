/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.JenkinsDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Level;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 * Importer for Jenkins usage statistics.
 * @author Leon Helwerda
 */
public class ImpJenkins extends BaseImport {

    @Override
    public void parser() {
        int projectID = getProjectID();
        JSONParser parser = new JSONParser();
        try (
            JenkinsDb jenkinsDb = new JenkinsDb();
            FileReader fr = new FileReader(getPath()+getProjectName()+"/data_jenkins.json");
        ) {
            JSONObject jsonObject = (JSONObject) parser.parse(fr);
            String host = (String) jsonObject.get("host");
            Long jobs = (Long) jsonObject.get("jobs");
            Long views = (Long) jsonObject.get("views");
            Long nodes = (Long) jsonObject.get("nodes");
            
            int num_jobs = jobs.intValue();
            int num_views = views.intValue();
            int num_nodes = nodes.intValue();
            
            JenkinsDb.CheckResult result = jenkinsDb.check(projectID, host, num_jobs, num_views, num_nodes);
            if (result == JenkinsDb.CheckResult.MISSING) {
                jenkinsDb.insert(projectID, host, num_jobs, num_views, num_nodes);
            }
            else if (result == JenkinsDb.CheckResult.DIFFERS) {
                jenkinsDb.update(projectID, host, num_jobs, num_views, num_nodes);
            }
        }
        catch (FileNotFoundException ex) {
            getLogger().log(Level.WARNING, "Cannot import {0}: {1}", new Object[]{getImportName(), ex.getMessage()});
        }
        catch (Exception ex) {
            logException(ex);
        }
    }

    @Override
    public String getImportName() {
        return "Jenkins usage statistics";
    }
    
}
