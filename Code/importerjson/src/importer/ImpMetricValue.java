/**
 * Quality metric value importer.
 * 
 * Copyright 2017-2020 ICTU
 * Copyright 2017-2022 Leiden University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package importer;

import util.BaseImport;
import dao.MetricDb;
import dao.MetricDb.MetricName;
import dao.SprintDb;
import java.beans.PropertyVetoException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
        public static final int BUFFER_SIZE = 65536;
        private MetricDb mDB = null;
        private SprintDb sprintDb = null;
        private final File path;
        private final int projectID;
        
        public MetricCollector(File path, int projectID) {
            this.mDB = new MetricDb();
            this.sprintDb = new SprintDb();
            this.path = path;
            this.projectID = projectID;
        }
        
        private void readPath(String path) throws IOException, MetricReadException, SQLException, PropertyVetoException {
            // Parse the path, consisting of the actual location followed by
            // a fragment identifier (#), after which we have a list of flags,
            // delimited by vertical bars (|). The first flag is always the
            // tracker record from which to start reading the history file.
            // Other flags are "compact" to use a compact history reader, and
            // "local" to indicate that the path is a local file.
            String[] parts = path.split("#");
            String location = parts[0];
            List<String> flags = Arrays.asList(parts[1].split("\\|"));
            
            MetricReader reader;
            if (flags.contains("compact")) {
                reader = new CompactHistoryReader(this);
            }
            else {
                reader = new HistoryReader(this);
            }
            
            reader.parseFragment(flags.get(0));
            
            // For compatibility, locations ending in a vertical bar are local.
            boolean local;
            if (location.endsWith("|")) {
                location = location.substring(0, location.length() - 1);
                local = true;
            }
            else {
                local = flags.contains("local");
            }
            boolean compression = flags.contains("compression=gz");
            if (!compression && !flags.contains("compression=")) {
                compression = location.endsWith(".gz");
            }
            
            if (local) {
                readLocal(reader, location, compression);
            }
            else {
                readNetworked(reader, new URL(location), compression);
            }
        }
        
        public void readLocal(MetricReader reader, String path, boolean compression) throws IOException, MetricReadException, SQLException, PropertyVetoException {
            try (
                InputStream is = new FileInputStream(path);
                InputStream gis = compression ? new GZIPInputStream(is, BUFFER_SIZE) : is
            ) {
                reader.read(gis);
            }
        }

        public void readNetworked(MetricReader reader, URL url, boolean compression) throws IOException, MetricReadException, SQLException, PropertyVetoException {
            URLConnection con = url.openConnection();
            con.connect();
            try (
                InputStream is = con.getInputStream();
                InputStream gis = compression ? new GZIPInputStream(is, BUFFER_SIZE) : is
            ) {
                reader.read(gis);
            }
        }
        
        public void readBufferedJSON(BufferedJSONReader br) throws IOException, MetricReadException, SQLException, PropertyVetoException {
            Object object;
            try {
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
            catch (ParseException ex) {
                throw new MetricReadException("Could not read JSON", ex);
            }
        }

        private void handleObject(JSONObject jsonObject) throws SQLException, PropertyVetoException {
            String metric_name = (String) jsonObject.get("name");
            String base_name = (String) jsonObject.get("base_name");
            String domain_name = (String) jsonObject.get("domain_name");
            String domain_type = (String) jsonObject.get("domain_type");
            String value = (String) jsonObject.get("value");
            String category = (String) jsonObject.get("category");
            String date = (String) jsonObject.get("date");
            String since_date = (String) jsonObject.get("since_date");
            MetricName nameParts = null;
            if (base_name != null && domain_name != null) {
                nameParts = new MetricName(metric_name, base_name, domain_name, domain_type);
            }

            insert(metric_name, Integer.parseInt(value), category, Timestamp.valueOf(date), Timestamp.valueOf(since_date), nameParts);
        }

        public void insert(String metric_name, float value, String category, Timestamp date, Timestamp since_date, MetricName nameParts) throws SQLException, PropertyVetoException {
            // Using the metric name, check if the metric was not already stored
            MetricName metricName = mDB.check_metric(metric_name);

            if (metricName == null) {
                if (nameParts == null) {
                    nameParts = mDB.split_metric_name(metric_name, false);
                }
                // Check the metric name after splitting because the original name
                // may have been altered.
                metricName = mDB.check_metric(nameParts.getName());
                if (metricName == null) {
                    mDB.insert_metric(nameParts);
                    metricName = mDB.check_metric(nameParts.getName(), true);
                    if (metricName == null) {
                        throw new ImporterException("Could not determine ID for metric name");
                    }
                }
            }
            else if (!metric_name.equals(metricName.getName()) || (nameParts != null && !metricName.equals(nameParts))) {
                if (nameParts == null) {
                    nameParts = mDB.split_metric_name(metric_name, false);
                }
                mDB.update_metric(metricName.getId(), metricName.getName(), nameParts);
            }
            
            int sprint_id = sprintDb.find_sprint(projectID, date);

            mDB.insert_metricValue(metricName.getId(), value, category, date, sprint_id, since_date, projectID);
        }
        
        public void insert(String metric_name, float value, String category, Timestamp date, Timestamp since_date) throws SQLException, PropertyVetoException {
            insert(metric_name, value, category, date, since_date, null);
        }
        
        @Override
        public void close() throws Exception {
            mDB.close();
            sprintDb.close();
        }

        public File getPath() {
            return path;
        }
    }
    
    private static class MetricReadException extends Exception {
        private MetricReadException(String message) {
            super(message);
        }
        
        private MetricReadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    private abstract static class MetricReader {
        protected final MetricCollector collector;
        protected final static Logger LOGGER = Logger.getLogger("importer");
        
        public MetricReader(MetricCollector collector) {
            this.collector = collector;
        }
        
        public abstract void parseFragment(String fragment);
        public abstract void read(InputStream is) throws IOException, MetricReadException, SQLException, PropertyVetoException;
    }
    
    private final static class HistoryReader extends MetricReader {
        public static final int BUFFER_SIZE = 65536;
        private int start_from = 0;
        private final StringReplacer replacer;
        private final JSONParser parser = new JSONParser();

        public HistoryReader(MetricCollector collector) {
            super(collector);
            replacer = new StringReplacer();
            replacer.add("(\"", "[\"").add("\")", "\"]").add(", }", "}");
        }

        @Override
        public void parseFragment(String fragment) {
            try {
                start_from = Integer.parseInt(fragment);
            }
            catch (NumberFormatException ex) {
                LOGGER.logp(Level.FINE, "HistoryReader", "parseFragment", "Fragment cannot be parsed", ex);
            }
        }
        
        @Override
        public void read(InputStream is) throws IOException, MetricReadException, SQLException, PropertyVetoException {
            int line_count = 0;
            Boolean success = false;
            try (
                Reader reader = new InputStreamReader(is, "UTF-8");
                BufferedReader br = new BufferedReader(reader, BUFFER_SIZE)
            ) {
                String line;
                JSONObject metric_row;
                while ((line = br.readLine()) != null) {
                    line_count++;
                    if (line_count <= start_from || line.isEmpty()) {
                        continue;
                    }
                    
                    metric_row = parseRow(line);
                    
                    Timestamp date = parseDate(metric_row);
                    if (date != null) {
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
                }
                success = true;
            }
            catch (MetricReadException e) {
                throw new MetricReadException("Problem at line " + line_count, e);
            }
            finally {
                // Write a progress file so that we can read from the correct location.
                // Do this upon midway failure as well, but not if we did not read further than the start line.
                // If we failed somewhere midway, then next time start from the line we failed on.
                if (line_count > start_from) {
                    try (PrintWriter writer = new PrintWriter(new File(collector.getPath(), "history_line_count.txt"))) {
                        writer.println(String.valueOf(success ? line_count : line_count-1));
                    }
                }
            }
        }
        
        private JSONObject parseRow(String line) throws MetricReadException {
            String row = replacer.execute(line);
            try {
                return (JSONObject) parser.parse(row);
            }
            catch (ParseException e) {
                throw new MetricReadException("Could not parse row:\n" + row, e);
            }
        }
        
        private Timestamp parseDate(JSONObject metric_row) {
            try {
                return Timestamp.valueOf((String) metric_row.get("date"));
            }
            catch (IllegalArgumentException ex) {
                LOGGER.logp(Level.SEVERE, "HistoryReader", "parseDate", "Date parsing exception", ex);
                return null;
            }
        }
    }
    
    private final static class CompactHistoryReader extends MetricReader {
        private String max_record_time = "";
        private String current_record_time = "";

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
                    LOGGER.logp(Level.FINE, "CompactHistoryReader", "parseFragment", "Could not parse record time from fragment");
                }
            }
            current_record_time = max_record_time;
        }
        
        @Override
        public void read(InputStream is) throws IOException, MetricReadException, SQLException, PropertyVetoException {
            JSONParser parser = new JSONParser();
            try (Reader reader = new InputStreamReader(is, "UTF-8")) {
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
                    try (PrintWriter writer = new PrintWriter(new File(collector.getPath(), "history_record_time.txt"))) {
                        writer.println(String.valueOf(max_record_time));
                    }
                }
            }
            catch (ParseException ex) {
                throw new MetricReadException("Cannot parse JSON object", ex);
            }
        }
        
        private String[] parseDates(JSONArray dateArray, int length) {
            List<String> dateList = new ArrayList<>();
            for (Object item : dateArray) {
                dateList.add((String)item);
            }
            return dateList.toArray(new String[length]);
        }

        private boolean parseMetric(String metric_name, JSONArray data, String[] dates, int max_index) throws MetricReadException, SQLException, PropertyVetoException {
            int previous_index = 0;
            for (Object record : data) {
                JSONObject measurement = (JSONObject) record;
                String start_time = (String) measurement.get("start");
                String end_time = (String) measurement.get("end");
                String status = (String) measurement.get("status");
                Object value = measurement.get("value");
                float metric_value;
                if (value == null) {
                    metric_value = -1;
                }
                else if (value instanceof Long) {
                    metric_value = ((Long) value).floatValue();
                }
                else if (value instanceof Double) {
                    metric_value = ((Double) value).floatValue();
                }
                else {
                    throw new MetricReadException("Unexpected type of value: " + value.getClass().getSimpleName());
                }

                Timestamp since_date = Timestamp.valueOf(start_time);

                // Search for the indexes of the dates that correspond with the
                // start and end dates, such that we can loop over this range to
                // add all measurement dates. For the start time, use right
                // bisection if the start time is set from the max record time.
                // In all other cases, use left bisection.

                int start_index;
                if (start_time.compareTo(current_record_time) < 0) {
                    start_time = current_record_time;
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
                    collector.insert(metric_name, metric_value, status, Timestamp.valueOf(date), since_date);
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
    public void parser() {
        File exportPath = getExportPath();
        int i = 0;
        for (String file : getImportFiles()) {
            File path = new File(exportPath, file);
            try (MetricCollector collector = new MetricCollector(exportPath, getProjectID())) {
                if (i == 0) {
                    // Read metrics JSON using buffered readers so that Java does not run out of memory
                    try (BufferedJSONReader br = new BufferedJSONReader(new FileReader(path))) {
                        collector.readBufferedJSON(br);
                    }
                }
                else {
                    // Read additional JSON files as compact history files.
                    collector.readLocal(new CompactHistoryReader(collector), path.toString(), false);
                }
            }
            catch (FileNotFoundException ex) {
                getLogger().log(Level.WARNING, "Cannot import {0}: {1}", new Object[]{getImportName(), ex.getMessage()});
            }
            catch (Exception ex) {
                logException(ex);
            }
            finally {
                i++;
            }
        }
    }

    @Override
    public String getImportName() {
        return "metric values";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_metrics.json", "data_history.json"};
    }

}
