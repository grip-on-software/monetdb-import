/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import util.BaseImport;
import dao.MetricDb;
import dao.MetricDb.MetricName;
import dao.SprintDb;
import java.beans.PropertyVetoException;
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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import util.Bisect;
import util.BufferedJSONReader;
import util.StringReplacer;

/**
 * Importer for metric values.
 * @author Enrique
 */
public class ImpMetricValue extends BaseImport {
    private static class MetricCollector implements AutoCloseable {
        private MetricDb mDB = null;
        private SprintDb sprintDb = null;
        private final String path;
        private final int projectID;
        
        public MetricCollector(String path, int projectID) {
            this.mDB = new MetricDb();
            this.sprintDb = new SprintDb();
            this.path = path;
            this.projectID = projectID;
        }
        
        private void readPath(String path) throws Exception {
            MetricReader reader;
            if (path.contains("compact-history")) {
                reader = new CompactHistoryReader(this);
            }
            else {
                reader = new HistoryReader(this);
            }
            
            if (path.contains("|")) {
                readLocal(reader, path);
            }
            else {
                readNetworked(reader, new URL(path));
            }
        }
        
        private void readLocal(MetricReader reader, String path) throws Exception {
            String[] parts = path.split("\\|");
            String filename = parts[0];
            reader.parseFragment(parts[1].substring(1));
            try (InputStream is = new FileInputStream(filename)) {
                reader.read(is);
            }
        }

        private void readNetworked(MetricReader reader, URL url) throws Exception {
            String fragment = url.getRef();
            reader.parseFragment(fragment);
            
            URLConnection con = url.openConnection();
            con.connect();
            try (InputStream is = con.getInputStream()) {
                reader.read(is);
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

            insert(metric_name, Integer.parseInt(value), category, Timestamp.valueOf(date), Timestamp.valueOf(since_date));
        }

        public void insert(String metric_name, int value, String category, Timestamp date, Timestamp since_date) throws Exception {
            // Using the metric name, check if the metric was not already stored
            int metric_id = mDB.check_metric(metric_name);

            if (metric_id == 0) {
                MetricName nameParts = mDB.split_metric_name(metric_name, false);
                // Check the metric name after splitting because the original name
                // may have been altered.
                metric_id = mDB.check_metric(nameParts.getName());
                if (metric_id == 0) {
                    mDB.insert_metric(nameParts);
                    metric_id = mDB.check_metric(nameParts.getName(), true);
                    if (metric_id == 0) {
                        throw new Exception("Could not determine ID for metric name");
                    }
                }
            }
            
            int sprint_id = sprintDb.find_sprint(projectID, date);

            mDB.insert_metricValue(metric_id, value, category, date, sprint_id, since_date, projectID);
        }
        
        @Override
        public void close() throws Exception {
            mDB.close();
            sprintDb.close();
        }

        public String getPath() {
            return path;
        }
    }
    
    private abstract static class MetricReader {
        public static final int BUFFER_SIZE = 65536;
        protected final MetricCollector collector;
        protected final static Logger LOGGER = Logger.getLogger("importer");
        
        public MetricReader(MetricCollector collector) {
            this.collector = collector;
        }
        
        public abstract void parseFragment(String fragment);
        public abstract void read(InputStream is) throws Exception;
    }
    
    private final static class HistoryReader extends MetricReader {
        private int start_from = 0;

        public HistoryReader(MetricCollector collector) {
            super(collector);
        }

        @Override
        public void parseFragment(String fragment) {
            try {
                start_from = Integer.parseInt(fragment);
            }
            catch (NumberFormatException ex) {
            }
        }
        
        @Override
        public void read(InputStream is) throws Exception {
            JSONParser parser = new JSONParser();
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
                        LOGGER.logp(Level.SEVERE, "MetricReader", "readGzip", "Date parsing exception", ex);
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

                            collector.insert(metric_name, Integer.parseInt(value), category, date, since_date);
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
                    try (PrintWriter writer = new PrintWriter(collector.getPath()+"/history_line_count.txt")) {
                        writer.println(String.valueOf(success ? line_count : line_count-1));
                    }
                }
            }
        }
    }
    
    private final static class CompactHistoryReader extends MetricReader {
        private String max_record_time = "";

        public CompactHistoryReader(MetricCollector collector) {
            super(collector);
        }
        
        @Override
        public void parseFragment(String fragment) {
            if ("0".equals(fragment)) {
                try {
                    Timestamp latest_date = collector.mDB.get_latest_metric_date(collector.projectID);
                    if (latest_date != null) {
                        max_record_time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(latest_date);
                    }
                } catch (SQLException | PropertyVetoException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
            else {
                try {
                    Timestamp.valueOf(fragment);
                    max_record_time = fragment;
                }
                catch (IllegalArgumentException ex) {

                }
            }
        }
        
        @Override
        public void read(InputStream is) throws Exception {
            JSONParser parser = new JSONParser();
            try (
                GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
                Reader reader = new InputStreamReader(gis, "UTF-8");
            ) {
                JSONObject object = (JSONObject) parser.parse(reader);
                
                JSONArray dateArray = (JSONArray) object.get("dates");
                int max_index = dateArray.size();
                String[] dates = parseDates(dateArray, max_index);
                JSONObject metrics = (JSONObject) object.get("metrics");
                
                for (Iterator it = metrics.entrySet().iterator(); it.hasNext();) {
                    Map.Entry pair = (Map.Entry)it.next();
                    String metric_name = (String) pair.getKey();
                    JSONArray data = (JSONArray) pair.getValue();
                    
                    if (!parseMetric(metric_name, data, dates, max_index)) {
                        break;
                    }
                }
                
                // Write a progress file so that we can read new metric value
                // additions later on. Only do this if the import succeeds.
                if (!max_record_time.isEmpty()) {
                    try (PrintWriter writer = new PrintWriter(collector.getPath()+"/history_record_time.txt")) {
                        writer.println(String.valueOf(max_record_time));
                    }
                }
            }
        }
        
        private String[] parseDates(JSONArray dateArray, int length) {
            List<String> dateList = new ArrayList<>();
            for (Object item : dateArray) {
                dateList.add((String)item);
            }
            return dateList.toArray(new String[length]);
        }

        private boolean parseMetric(String metric_name, JSONArray data, String[] dates, int max_index) throws Exception {
            int previous_index = 0;
            for (Object record : data) {
                JSONObject measurement = (JSONObject) record;
                String start_time = (String) measurement.get("start");
                String end_time = (String) measurement.get("end");
                String status = (String) measurement.get("status");
                int value = (int) measurement.getOrDefault("value", -1);

                Timestamp since_date = Timestamp.valueOf(start_time);

                // Search for the indexes of the dates that correspond with the
                // start and end dates, such that we can loop over this range to
                // add all measurement dates. For the start time, use right
                // bisection if the start time is set from the max record time.
                // In all other cases, use left bisection.

                int start_index;
                if (start_time.compareTo(max_record_time) < 0) {
                    start_time = max_record_time;
                    start_index = Bisect.bisectRight(dates, start_time, previous_index, max_index);
                }
                else {
                    start_index = Bisect.bisectLeft(dates, start_time, previous_index, max_index);
                }
                if (start_index >= max_index) {
                    LOGGER.log(Level.INFO, "Start time {0} with index {1} out of range ({2}, {3})", new Object[]{start_time, start_index, previous_index, max_index});
                    return false;
                }
                
                int end_index = Bisect.bisectLeft(dates, end_time, start_index, max_index);
                
                for (int i = start_index; i < end_index; i++) {
                    String date = dates[i];
                    collector.insert(metric_name, value, status, Timestamp.valueOf(date), since_date);
                }

                // Track latest date indices and new dates.
                previous_index = end_index;
                if (end_time.compareTo(max_record_time) > 0) {
                    max_record_time = end_time;
                }
                
                if (end_index >= max_index) {
                    LOGGER.log(Level.WARNING, "End time {0} with index {1} went out of range, max index is {2}", new Object[]{end_time, end_index, max_index});
                    return true;
                }
            }
            
            return true;
        }
    }

    @Override
    public void parser(){
        String path = getPath()+getProjectName();

        try (
            MetricCollector collector = new MetricCollector(path, this.getProjectID());
            // Read metrics JSON using buffered readers so that Java does not run out of memory
            BufferedJSONReader br = new BufferedJSONReader(new FileReader(path+"/data_metrics.json"))
        ) {
            collector.readBufferedJSON(br);
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
    
