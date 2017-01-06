/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import util.BaseImport;
import dao.MetricDb;
import java.io.BufferedReader;
import java.io.FileReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Enrique
 */
public class ImpMetricValue extends BaseImport{
    
    BufferedReader br = null;
    JSONParser parser = new JSONParser();

    public void parser(Integer projectID, String projectN){

        int metric_id = 0;
        int i = 0;
        MetricDb mDB;
        
        try {
                 
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+projectN+"/data_metrics.json"));
     
            for (Object o : a)
            {
     
                JSONObject jsonObject = (JSONObject) o;
                
                String metric_name = (String) jsonObject.get("name");
                String value = (String) jsonObject.get("value");
                String category = (String) jsonObject.get("category");
                String sdate = (String) jsonObject.get("date");
                
                //with the metric name check if the metric was already stored
                mDB = new MetricDb();
                metric_id = mDB.check_metric(metric_name);
            
                if(metric_id == 0){

                    mDB.insert_metric(metric_name);
                    metric_id = mDB.check_metric(metric_name);
                    
                }
                
                mDB.insert_metricValue(metric_id, Integer.parseInt(value), category, sdate, projectID);
                i++;
                if (i % 1000 == 0) {
                    mDB.execute_metricValue();
                }
            }
       
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
        

}
    
