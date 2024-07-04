/**
 * Quality metric target importer.
 * 
 * Copyright 2017-2020 ICTU
 * Copyright 2017-2022 Leiden University
 * Copyright 2017-2023 Leon Helwerda
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

import dao.DataSource;
import dao.MetricDb;
import dao.MetricDb.MetricName;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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
 
        try (
            MetricDb metricDb = new MetricDb();
            FileReader fr = new FileReader(getMainImportPath())
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                String name = (String) jsonObject.get("name");
                String target = (String) jsonObject.get("target");
                String low_target = (String) jsonObject.get("low_target");
                String comment = (String) jsonObject.get("comment");
                String revision = (String) jsonObject.get("revision");
                String base_name = (String) jsonObject.get("base_name");
                String domain_name = (String) jsonObject.get("domain_name");
                String unchanged = (String) jsonObject.get("default");
                String debt_target = (String) jsonObject.get("debt_target");
                String domain_type = (String) jsonObject.get("domain_type");
                String scale = (String) jsonObject.get("scale");
                String direction = (String) jsonObject.get("direction");
                
                MetricName metricName = metricDb.check_metric(name);
                if (metricName == null) {
                    metricDb.insert_metric(new MetricName(name, base_name, domain_name, domain_type));
                    metricName = metricDb.check_metric(name, true);
                }

                if ("".equals(target)) {
                    target = "0";
                }
                if ("".equals(low_target)) {
                    low_target = "0";
                }
                if ("".equals(debt_target)) {
                    debt_target = null;
                }
                
                if (unchanged == null || "0".equals(unchanged)) {
                    metricDb.insert_target(projectId, revision, metricName.getId(), comment, parseDirection(direction), parseTarget(target), parseTarget(low_target), parseTarget(debt_target), scale);
                }
            }            
        }
        catch (FileNotFoundException ex) {
            getLogger().log(Level.WARNING, "Cannot import {0}: {1}", new Object[]{getImportName(), ex.getMessage()});
        }
        catch (Exception ex) {
            logException(ex);
        }

    }
    
    public void updateDomainNames() {
        String sql = "SELECT metric_id, name FROM gros.metric WHERE (base_name IS NULL AND domain_name IS NULL) OR (base_name <> name AND domain_name = '')";
        try (
            MetricDb metricDb = new MetricDb();
            Connection con = DataSource.getInstance().getConnection();
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
        ) {
            loadBaseNames(metricDb);
            while (rs.next()) {
                int metric_id = rs.getInt("metric_id");
                String metric_name = rs.getString("name");
                MetricName nameParts = metricDb.split_metric_name(metric_name, true);
                if (nameParts.getBaseName() != null && nameParts.getDomainName() != null) {
                    MetricName other_metric = metricDb.check_metric(nameParts.getName());
                    if (other_metric != null && other_metric.getId() != metric_id) {
                        metricDb.delete_metric(metric_id, metric_name, other_metric.getId());
                    }
                    else {
                        metricDb.update_metric(metric_id, metric_name, nameParts);
                    }
                }
            }
        }
        catch (Exception ex) {
            logException(ex);
        }
    }

    private void loadBaseNames(MetricDb metricDb) throws ParseException, PropertyVetoException, SQLException {
        try (FileReader fr = new FileReader(new File(getRootPath().toFile(), "metrics_base_names.json"))) {
            JSONParser parser = new JSONParser();
            JSONArray a = (JSONArray) parser.parse(fr);
            List<String> base_names = new ArrayList<>();
            for (Object name : a) {
                base_names.add((String)name);
            }
            
            metricDb.load_metric_base_names(base_names);
        }
        catch (IOException ex) {
            getLogger().log(Level.WARNING, "Cannot load extra base names: {0}", ex.getMessage());
        }
    }
    
    public void updateDefaultTargets() {
        JSONParser parser = new JSONParser();
 
        try (
            MetricDb metricDb = new MetricDb();
            FileReader fr = new FileReader(new File(getExportPath(), "data_metric_defaults.json"))
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                String base_name = (String) jsonObject.get("base_name");
                String revision = (String) jsonObject.get("version_id");
                String commit_date = (String) jsonObject.get("commit_date");
                String direction = (String) jsonObject.get("direction");
                String perfect = (String) jsonObject.get("perfect_value");
                String target = (String) jsonObject.get("target_value");
                String low_target = (String) jsonObject.get("low_target_value");
                String scale = (String) jsonObject.get("scale");
                
                Timestamp date = Timestamp.valueOf(commit_date);

                if (!metricDb.check_default_target(base_name, revision)) {
                    metricDb.insert_default_target(base_name, revision, date, parseDirection(direction), parseTarget(perfect), parseTarget(target), parseTarget(low_target), scale);
                }
            }            
        }
        catch (FileNotFoundException ex) {
            getLogger().log(Level.WARNING, "Cannot import metric default targets: {0}", ex.getMessage());
        }
        catch (Exception ex) {
            logException(ex);
        }        
    }

    protected Float parseTarget(String value) {
        Float target = null;
        if (value != null) {
            try {
                target = Float.parseFloat(value);
            }
            catch (NumberFormatException ex) {
                getLogger().log(Level.WARNING, "Invalid value for target", ex);
            }
        }
        return target;
    }

    protected Boolean parseDirection(String value) {
        Boolean higherIsBetter = null;
        if ("1".equals(value)) {
            higherIsBetter = true;
        }
        else if ("-1".equals(value)) {
            higherIsBetter = false;
        }
        return higherIsBetter;
    }

    @Override
    public String getImportName() {
        return "metric targets";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_metric_targets.json"};
    }

}
