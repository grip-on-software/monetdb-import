/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.BatchedStatement;
import dao.DataSource;
import dao.MetricDb;
import dao.MetricDb.MetricName;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
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
            FileReader fr = new FileReader(getPath()+getProjectName()+"/data_metric_targets.json")
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
                    metricDb.insert_metric(name, base_name, domain_name);
                    metric_id = metricDb.check_metric(name, true);
                }
                
                metricDb.insert_target(projectId, Integer.parseInt(revision), metric_id, type, Integer.parseInt(target), Integer.parseInt(low_target), comment);
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
        String updateSql = "UPDATE gros.metric SET base_name = ?, domain_name = ? WHERE metric_id = ?";
        try (
            MetricDb metricDb = new MetricDb();
            Connection con = DataSource.getInstance().getConnection();
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            BatchedStatement bstmt = new BatchedStatement(updateSql)
        ) {
            while (rs.next()) {
                PreparedStatement pstmt = bstmt.getPreparedStatement();
                int metric_id = rs.getInt("metric_id");
                String metric_name = rs.getString("name");
                MetricName nameParts = metricDb.split_metric_name(metric_name);
                if (nameParts.getBaseName() != null && nameParts.getDomainName() != null) {
                    pstmt.setString(1, nameParts.getBaseName());
                    pstmt.setString(2, nameParts.getDomainName());
                    pstmt.setInt(3, metric_id);
                    
                    bstmt.batch();
                }
            }
            
            bstmt.execute();
        }
        catch (Exception ex) {
            logException(ex);
        }
    }

    @Override
    public String getImportName() {
        return "metric targets";
    }
}
