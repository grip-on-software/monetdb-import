/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import util.BaseImport;
import dao.MetricDb;
import dao.SprintDb;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import util.BufferedJSONReader;
import util.StringReplacer;

/**
 * Importer for metric values.
 * @author Enrique
 */
public class ImpMetricValue extends BaseImport {
    private static class MetricReader implements AutoCloseable {
        private final JSONParser parser = new JSONParser();
        private MetricDb mDB = null;
        private SprintDb sprintDb = null;
        private final String path;
        private final int projectID;
        private static final int BUFFER_SIZE = 65536;
        
        public MetricReader(String path, int projectID) {
            this.mDB = new MetricDb();
            this.sprintDb = new SprintDb();
            this.path = path;
            this.projectID = projectID;
        }
        
        private void readPath(String path) throws Exception {
            if (path.contains("|")) {
                String[] parts = path.split("\\|");
                String filename = parts[0];
                int start_from = Integer.parseInt(parts[1].substring(1));
                try (InputStream is = new FileInputStream(filename)) {
                    readGzip(is, start_from);
                }
            }
            else {
                readNetworked(new URL(path));
            }
        }

        private void readNetworked(URL url) throws Exception {
            String fragment = url.getRef();
            int start_from = Integer.parseInt(fragment);
            
            URLConnection con = url.openConnection();
            con.connect();
            try (InputStream is = con.getInputStream()) {
                readGzip(is, start_from);
            }
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
                StringReplacer replacer = new StringReplacer();
                replacer.add("(\"", "[\"").add("\")", "\"]").add(", }", "}");
                while ((line = br.readLine()) != null) {
                    line_count++;
                    if (line_count <= start_from) {
                        continue;
                    }
                    if (line.isEmpty()) {
                        continue;
                    }
                    
                    String row = replacer.execute(line);
                    try {
                        metric_row = (JSONObject) parser.parse(row);
                    }
                    catch (ParseException e) {
                        throw new Exception("Could not parse row:\n" + row, e);
                    }
                    
                    Timestamp date;
                    try {
                        date = Timestamp.valueOf((String) metric_row.get("date"));
                    }
                    catch (IllegalArgumentException ex) {
                        Logger.getLogger("importer").logp(Level.SEVERE, "MetricReader", "readGzip", "Date parsing exception", ex);
                        continue;
                    }
                    
                    for (Iterator it = metric_row.entrySet().iterator(); it.hasNext();) {
                        Map.Entry pair = (Map.Entry)it.next();
                        Object data = pair.getValue();
                        if (data instanceof JSONArray) {
                            JSONArray metric_data = (JSONArray) data;
                            String metric_name = (String) pair.getKey();
                            
                            String value = (String) metric_data.get(0);
                            String category = (String) metric_data.get(1);
                            
                            Timestamp since_date;
                            if (metric_data.size() > 2) {
                                String since_time = (String) metric_data.get(2);
                                since_date = Timestamp.valueOf(since_time);
                            }
                            else {
                                since_date = null;
                            }

                            insert(metric_name, value, category, date, since_date);
                        }
                    }
                }
                success = true;
            }
            catch (Exception e) {
                throw new Exception("Problem at line " + String.valueOf(line_count), e);
            }
            finally {
                // Write a progress file so that we can read from the correct location.
                // Do this upon midway failure as well, but not if we did not read further than the start line.
                // If we failed somewhere midway, then next time start from the line we failed on.
                if (line_count > start_from) {
                    try (PrintWriter writer = new PrintWriter(path+"/history_line_count.txt")) {
                        writer.println(String.valueOf(success ? line_count : line_count-1));
                    }
                }
            }
        }

        public void readBufferedJSON(BufferedJSONReader br) throws Exception {
            Object object;
            while ((object = br.readObject()) != null) {
                if (object instanceof String) {
                    readPath((String) object);
                    break;
                }
                else {
                    handleObject((JSONObject) object);
                }
            }
        }

        private void handleObject(JSONObject jsonObject) throws Exception {        
            String metric_name = (String) jsonObject.get("name");
            String value = (String) jsonObject.get("value");
            String category = (String) jsonObject.get("category");
            String date = (String) jsonObject.get("date");
            String since_date = (String) jsonObject.get("since_date");

            insert(metric_name, value, category, Timestamp.valueOf(date), Timestamp.valueOf(since_date));
        }

        private void insert(String metric_name, String value, String category, Timestamp date, Timestamp since_date) throws Exception {
            // Using the metric name, check if the metric was not already stored
            int metric_id = mDB.check_metric(metric_name);

            if (metric_id == 0){
                mDB.insert_metric(metric_name);
                metric_id = mDB.check_metric(metric_name);
                if (metric_id == 0) {
                    throw new Exception("could not determine metric name");
                }
            }
            
            int sprint_id = sprintDb.find_sprint(projectID, date);

            mDB.insert_metricValue(metric_id, Integer.parseInt(value), category, date, sprint_id, since_date, projectID);
        }

        @Override
        public void close() throws Exception {
            mDB.close();
            sprintDb.close();
        }
    }

    @Override
    public void parser(){
        String path = getPath()+getProjectName();

        try (
            MetricReader reader = new MetricReader(path, this.getProjectID());
            // Read metrics JSON using buffered readers so that Java does not run out of memory
            BufferedJSONReader br = new BufferedJSONReader(new FileReader(path+"/data_metrics.json"))
        ) {
            reader.readBufferedJSON(br);
        }
        catch (FileNotFoundException ex) {
            getLogger().log(Level.WARNING, "Cannot import {0}: {1}", new Object[]{getImportName(), ex.getMessage()});
        }
        catch (Exception ex) {
            logException(ex);
        }
        
    }

    @Override
    public String getImportName() {
        return "metric values";
    }

}
    
