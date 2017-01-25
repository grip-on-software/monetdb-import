/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.MetricDb;
import java.io.FileReader;
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
        MetricDb metricDb = new MetricDb();
        int projectId = this.getProjectID();
        int version_id = 0;
 
        try {
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+getProjectName()+"/data_metric_version.json"));
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String message = (String) jsonObject.get("message");
                String developer = (String) jsonObject.get("developer");
                String version = (String) jsonObject.get("version");
                String commit_date = (String) jsonObject.get("commit_date");
                
                version_id = metricDb.check_version(projectId, Integer.parseInt(version));
            
                if(version_id == 0){

                    metricDb.insert_version(projectId, Integer.parseInt(version), message, developer, commit_date);
                    
                }
            }
            
            metricDb.close();
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }

    }
}
