/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import util.BaseImport;
import dao.MetricDb;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.SQLException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Enrique
 */
public class ImpMetricValue extends BaseImport{
    
    BufferedReader br = null;
    JSONParser parser = new JSONParser();
    MetricDb mDB = null;
    
    private void handleObject(JSONObject jsonObject) throws Exception {
        int metric_id = 0;
        
        String metric_name = (String) jsonObject.get("name");
        String value = (String) jsonObject.get("value");
        String category = (String) jsonObject.get("category");
        String date = (String) jsonObject.get("date");
        String since_date = (String) jsonObject.get("since_date");


        // Using the metric name, check if the metric was not already stored
        metric_id = mDB.check_metric(metric_name);

        if(metric_id == 0){
            mDB.insert_metric(metric_name);
            metric_id = mDB.check_metric(metric_name);
            if (metric_id == 0) {
                throw new Exception("could not determine metric name");
            }
        }

        mDB.insert_metricValue(metric_id, Integer.parseInt(value), category, date, since_date, this.getProjectID());
    }

    @Override
    public void parser(){

        mDB = new MetricDb();
        
        try {
                 
            // Read metrics JSON using buffered readers so that Java does not run out of memory
            br = new BufferedReader(new FileReader(getPath()+getProjectName()+"/data_metrics.json"));
            
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                if ("[".equals(line) || "]".equals(line)) {
                    continue;
                }
                sb.append(line.trim());
                
                if (sb.length() > 2) {
                    if (sb.substring(sb.length()-2).equals("},")) {
                        String json = sb.substring(0, sb.length()-1);
                        JSONObject jsonObject = (JSONObject) parser.parse(json);
                        handleObject(jsonObject);
                    
                        sb.setLength(0);
                    }
                }
            }
            
            mDB.close();
        }
        catch (FileNotFoundException e) {
            System.out.println("Cannot import metrics: " + e.getMessage());
        }
        catch (SQLException e) {
            printSQLExceptionDetails(e);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
        

}
    
