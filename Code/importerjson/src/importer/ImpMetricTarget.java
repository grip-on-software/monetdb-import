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
                String type = (String) jsonObject.get("type");
                String comment = (String) jsonObject.get("comment");
                String revision = (String) jsonObject.get("revision");
                String base_name = (String) jsonObject.get("base_name");
                String domain_name = (String) jsonObject.get("domain_name");
                String unchanged = (String) jsonObject.get("default");
                String debt_target = (String) jsonObject.get("debt_target");
                String domain_type = (String) jsonObject.get("domain_type");
                
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
                
                if (unchanged == null || "0".equals(unchanged)) {
                    if (debt_target != null && !"".equals(debt_target)) {
                        type = "TechnicalDebtTarget";
                        target = debt_target;
                    }
                    metricDb.insert_target(projectId, revision, metricName.getId(), type, Integer.parseInt(target), Integer.parseInt(low_target), comment);
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
            FileReader fr = new FileReader(new File(getRootPath().toFile(), "data_hqlib.json"))
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                String base_name = (String) jsonObject.get("class_name");
                String revision = (String) jsonObject.get("version_id");
                String commit_date = (String) jsonObject.get("commit_date");
                String direction = (String) jsonObject.get("direction");
                String perfect = (String) jsonObject.get("perfect_value");
                String target = (String) jsonObject.get("target_value");
                String low_target = (String) jsonObject.get("low_target_value");
                
                Timestamp date = Timestamp.valueOf(commit_date);
                Boolean higherIsBetter = null;
                if ("1".equals(direction)) {
                    higherIsBetter = true;
                }
                else if ("-1".equals(direction)) {
                    higherIsBetter = false;
                }

                if (!metricDb.check_default_target(base_name, revision)) {
                    metricDb.insert_default_target(base_name, revision, date, higherIsBetter, parseTarget(perfect), parseTarget(target), parseTarget(low_target));
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

    @Override
    public String getImportName() {
        return "metric targets";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_metric_targets.json"};
    }
}
