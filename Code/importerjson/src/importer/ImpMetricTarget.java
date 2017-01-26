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
 * @author leonhelwerda
 */
public class ImpMetricTarget extends BaseImport {
    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        MetricDb metricDb = new MetricDb();
        int projectId = this.getProjectID();
        int metric_id = 0;
 
        try {
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+getProjectName()+"/data_metric_targets.json"));
            
            for (Object o : a)
            {
      
                JSONObject jsonObject = (JSONObject) o;
                
                String name = (String) jsonObject.get("name");
                String target = (String) jsonObject.get("target");
                String low_target = (String) jsonObject.get("low_target");
                String type = (String) jsonObject.get("type");
                String comment = (String) jsonObject.get("comment");
                String revision = (String) jsonObject.get("revision");
                
                metric_id = metricDb.check_metric(name);
                if (metric_id == 0) {
                    metricDb.insert_metric(name);
                    metric_id = metricDb.check_metric(name);
                }
                
                metricDb.insert_target(projectId, Integer.parseInt(revision), metric_id, type, Integer.parseInt(target), Integer.parseInt(low_target), comment);
            }
            
            metricDb.close();
            
        }
        catch (FileNotFoundException e) {
            System.out.println("Cannot import metric targets: " + e.getMessage());
        }
        catch (SQLException e) {
            printSQLExceptionDetails(e);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
}
