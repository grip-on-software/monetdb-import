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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Enrique
 */
public class ImpMetricValue extends BaseImport{
    class MetricReader {
        JSONParser parser = new JSONParser();
        MetricDb mDB = null;
        int projectID;
        final int BUFFER_SIZE = 65536;
        
        public MetricReader(MetricDb mDB, int projectID) {
            this.mDB = mDB;
            this.projectID = projectID;
        }

        private void readNetworked(URL url) throws Exception {
            String fragment = url.getRef();
            int start_from = Integer.parseInt(fragment);
            InputStream is = url.openStream();
            readGzip(is, start_from);
        }
        
        private void readGzip(InputStream is, int start_from) throws Exception {
            int line_count = 0;
            Boolean success = false;
            try (
                GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
                Reader reader = new InputStreamReader(gis, "UTF-8");
                BufferedReader br = new BufferedReader(reader, BUFFER_SIZE)
            ) {
                String line;
                JSONObject metric_row;
                while ((line = br.readLine()) != null) {
                    line_count++;
                    if (line_count <= start_from) {
                        continue;
                    }
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    String row = line.replace("(", "[").replace(")", "]").replace(", }", "}");
                    try {
                        metric_row = (JSONObject) parser.parse(row);
                    }
                    catch (ParseException e) {
                        throw new Exception("Could not parse row:\n" + row, e);
                    }
                    String date = (String) metric_row.get("date");
                    for (Iterator it = metric_row.entrySet().iterator(); it.hasNext();) {
                        Map.Entry pair = (Map.Entry)it.next();
                        String metric_name = (String) pair.getKey();
                        Object data = pair.getValue();
                        if (data instanceof JSONArray) {
                            JSONArray metric_data = (JSONArray) data;
                            String value = (String) metric_data.get(0);
                            String category = (String) metric_data.get(1);
                            String since_date = (String) metric_data.get(2);

                            insert(metric_name, value, category, date, since_date);
                        }
                    }
                }
                success = true;
            }
            finally {
                // Write a progress file so that we can read from the correct location.
                // Do this upon midway failure as well, but not if we did not read further than the start line.
                // If we failed somewhere midway, then next time start from the line we failed on.
                if (line_count > start_from) {
                    try (PrintWriter writer = new PrintWriter(getPath()+getProjectName()+"history_line_count.txt")) {
                        writer.println(String.valueOf(success ? line_count : line_count-1));
                    }
                }
            }
        }

        public void readBufferedJSON(BufferedReader br) throws Exception {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("\"") && line.endsWith("\"")) {
                    String url = (String) parser.parse(line);
                    readNetworked(new URL(url));
                    break;
                }
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
        }

        private void handleObject(JSONObject jsonObject) throws Exception {        
            String metric_name = (String) jsonObject.get("name");
            String value = (String) jsonObject.get("value");
            String category = (String) jsonObject.get("category");
            String date = (String) jsonObject.get("date");
            String since_date = (String) jsonObject.get("since_date");

            insert(metric_name, value, category, date, since_date);
        }

        private void insert(String metric_name, String value, String category, String date, String since_date) throws Exception {
            // Using the metric name, check if the metric was not already stored
            int metric_id = mDB.check_metric(metric_name);

            if (metric_id == 0){
                mDB.insert_metric(metric_name);
                metric_id = mDB.check_metric(metric_name);
                if (metric_id == 0) {
                    throw new Exception("could not determine metric name");
                }
            }

            mDB.insert_metricValue(metric_id, Integer.parseInt(value), category, date, since_date, projectID);
        }
    }

    @Override
    public void parser(){

        try (
            MetricDb mDB = new MetricDb();
            // Read metrics JSON using buffered readers so that Java does not run out of memory
            BufferedReader br = new BufferedReader(new FileReader(getPath()+getProjectName()+"/data_metrics.json"))
        ) {
            MetricReader reader = new MetricReader(mDB, this.getProjectID());
            reader.readBufferedJSON(br);
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
    
