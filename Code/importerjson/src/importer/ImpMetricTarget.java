/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.MetricDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 * Importer for metric targets.
 * @author Leon Helwerda
 */
public class ImpMetricTarget extends BaseImport {
    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        int projectId = this.getProjectID();
        int metric_id = 0;
 
        try (
            MetricDb metricDb = new MetricDb();
            FileReader fr = new FileReader(getPath()+getProjectName()+"/data_metric_targets.json")
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a) {
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
        return "metric targets";
    }
}
