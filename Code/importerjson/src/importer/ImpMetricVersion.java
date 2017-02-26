/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.MetricDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.SQLException;
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
            FileReader fr = new FileReader(getPath()+getProjectName()+"/data_metric_versions.json")
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String message = (String) jsonObject.get("message");
                String developer = (String) jsonObject.get("developer");
                String version = (String) jsonObject.get("version_id");
                String commit_date = (String) jsonObject.get("commit_date");
                
                version_id = metricDb.check_version(projectId, Integer.parseInt(version));
            
                if(version_id == 0){

                    metricDb.insert_version(projectId, Integer.parseInt(version), developer, message, commit_date);
                    
                }
            }            
        }
        catch (FileNotFoundException e) {
            System.out.println("Cannot import metric versions: " + e.getMessage());
        }
        catch (SQLException e) {
            printSQLExceptionDetails(e);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
}
