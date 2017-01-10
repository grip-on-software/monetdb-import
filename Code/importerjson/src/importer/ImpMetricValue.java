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
import java.sql.SQLException;
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
        MetricDb mDB = new MetricDb();
        
        try {
                 
            JSONArray a = (JSONArray) parser.parse(new FileReader(getPath()+projectN+"/data_metrics.json"));
     
            for (Object o : a)
            {
     
                JSONObject jsonObject = (JSONObject) o;
                
                String metric_name = (String) jsonObject.get("name");
                String value = (String) jsonObject.get("value");
                String category = (String) jsonObject.get("category");
                String sdate = (String) jsonObject.get("date");
                
                // Using the metric name, check if the metric was not already stored
                metric_id = mDB.check_metric(metric_name);
            
                if(metric_id == 0){
                    mDB.insert_metric(metric_name);
                    metric_id = mDB.check_metric(metric_name);
                    
                }
                
                mDB.insert_metricValue(metric_id, Integer.parseInt(value), category, sdate, projectID);
            }
            
            mDB.close();
       
            
        }
            
        catch (SQLException e) {
            printSQLExceptionDetails(e);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
        

}
    
