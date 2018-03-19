/*
 * Copyright 2017-2018 ICTU
 * 
 * Importer of gathered software development metrics for MonetDB
 */
package importer;

import dao.DataSource;
import dao.MetricDb;
import dao.MetricDb.MetricName;
import java.beans.PropertyVetoException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
        int metric_id = 0;
 
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
                String type = (String) jsonObject.get("type");
                String comment = (String) jsonObject.get("comment");
                String revision = (String) jsonObject.get("revision");
                String base_name = (String) jsonObject.get("base_name");
                String domain_name = (String) jsonObject.get("domain_name");
                
                metric_id = metricDb.check_metric(name);
                if (metric_id == 0) {
                    metricDb.insert_metric(new MetricName(name, base_name, domain_name));
                    metric_id = metricDb.check_metric(name, true);
                }
                
                metricDb.insert_target(projectId, revision, metric_id, type, Integer.parseInt(target), Integer.parseInt(low_target), comment);
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
        String sql = "SELECT metric_id, name FROM gros.metric WHERE base_name IS NULL AND domain_name IS NULL";
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
                    int other_metric_id = metricDb.check_metric(nameParts.getName());
                    if (other_metric_id != 0 && other_metric_id != metric_id) {
                        metricDb.delete_metric(metric_id, metric_name, other_metric_id);
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
        try (FileReader fr = new FileReader(getRootPath()+"/metrics_base_names.json")) {
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

    @Override
    public String getImportName() {
        return "metric targets";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_metric_targets.json"};
    }
}
