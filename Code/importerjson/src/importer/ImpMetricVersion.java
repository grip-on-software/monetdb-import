/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.MetricDb;
import dao.SprintDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Timestamp;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 *
 * @author Leon Helwerda
 */
public class ImpMetricVersion extends BaseImport {
    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        int projectId = this.getProjectID();
        int version_id = 0;
 
        try (
            MetricDb metricDb = new MetricDb();
            SprintDb sprintDb = new SprintDb();
            FileReader fr = new FileReader(getPath()+getProjectName()+"/data_metric_versions.json")
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String message = (String) jsonObject.get("message");
                String developer = (String) jsonObject.get("developer");
                String revision = (String) jsonObject.get("version_id");
                String date = (String) jsonObject.get("commit_date");
                
                int version = Integer.parseInt(revision);
                version_id = metricDb.check_version(projectId, version);
            
                if (version_id == 0) {
                    Timestamp commit_date = Timestamp.valueOf(date);
                    int sprint_id = sprintDb.find_sprint(projectId, commit_date);

                    metricDb.insert_version(projectId, version, developer, message, commit_date, sprint_id);
                    
                }
            }            
        }
        catch (FileNotFoundException ex) {
            System.out.println("Cannot import " + getImportName() + ": " + ex.getMessage());
        }
        catch (Exception ex) {
            logException(ex);
        }

    }

    @Override
    public String getImportName() {
        return "metric versions";
    }
}
