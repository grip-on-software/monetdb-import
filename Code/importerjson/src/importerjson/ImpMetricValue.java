/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importerjson;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Enrique
 */
public class ImpMetricValue {
    
    BufferedReader br = null;
    PreparedStatement pstmt = null;
    PreparedStatement pstmt2 = null;
    Connection con = null;
    Connection con2 = null;
    JSONParser parser = new JSONParser();
    Statement st = null;
    Statement st2 = null;
    ResultSet rs = null;
    ResultSet rs2 = null;


    public void parser(){

        int metric_id = 0;
        MetricDb mDB;
        
        try {
        
            JSONArray a = (JSONArray) parser.parse(new FileReader("/path/to/data.json"));

            
            for (Object o : a)
            {
     
                JSONObject jsonObject = (JSONObject) o;
                
                String metric_name = (String) jsonObject.get("name");
                String value = (String) jsonObject.get("value");
                String sdate = (String) jsonObject.get("date");
                
                //with the metric name check if the metric was already stored
                mDB = new MetricDb();
                metric_id = mDB.check_metric(metric_name);
            
                if(metric_id == 0){

                    mDB.insert_metric(metric_name);
                    metric_id = mDB.check_metric(metric_name);
                    
                }
                
                mDB.insert_metricValue(metric_id, value, sdate, 1);
                          
            }
       
            
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
        finally{
      
            
        }
        
    }
        

}
    
