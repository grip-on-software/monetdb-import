/**
 * Quality metric value importer.
 * 
 * Copyright 2017-2020 ICTU
 * Copyright 2017-2022 Leiden University
 * Copyright 2017-2024 Leon Helwerda
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
        
        public void readBufferedJSON(BufferedJSONReader br) throws IOException, MetricReadException, SQLException, PropertyVetoException {
            Object object;
            try {
                while ((object = br.readObject()) != null) {
                    handleObject((JSONObject) object);
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
            String start = (String) jsonObject.get("since_date");
            MetricName nameParts = null;
            if (base_name != null && domain_name != null) {
                nameParts = new MetricName(metric_name, base_name, domain_name, domain_type);
            }

            Timestamp since_date = null;
            if (start != null) {
                since_date = Timestamp.valueOf(start);
            }
            insert(metric_name, Float.parseFloat(value), category, Timestamp.valueOf(date), since_date, nameParts);
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

    @Override
    public void parser() {
        File exportPath = getExportPath();
        int i = 0;
        for (String file : getImportFiles()) {
            File path = new File(exportPath, file);
            try (MetricCollector collector = new MetricCollector(exportPath, getProjectID())) {
                // Read metrics JSON using buffered readers so that Java does not run out of memory
                try (BufferedJSONReader br = new BufferedJSONReader(new FileReader(path))) {
                    collector.readBufferedJSON(br);
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
        return new String[]{"data_metrics.json"};
    }

}
