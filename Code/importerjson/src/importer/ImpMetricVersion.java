/*
 * Copyright 2017-2018 ICTU
 * 
 * Importer of gathered software development metrics for MonetDB
 */
package importer;

import dao.MetricDb;
import dao.SprintDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Timestamp;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 * Importer for metric versions.
 * @author Leon Helwerda
 */
public class ImpMetricVersion extends BaseImport {
    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        int projectId = this.getProjectID();
        int version_id = 0;
 
        try (
            MetricDb metricDb = new MetricDb();
            SprintDb sprintDb = new SprintDb();
            FileReader fr = new FileReader(getMainImportPath())
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                String message = (String) jsonObject.get("message");
                String developer = (String) jsonObject.get("developer");
                String revision = (String) jsonObject.get("version_id");
                String date = (String) jsonObject.get("commit_date");
                
                version_id = metricDb.check_version(projectId, revision);
            
                if (version_id == 0) {
                    Timestamp commit_date = Timestamp.valueOf(date);
                    int sprint_id = sprintDb.find_sprint(projectId, commit_date);

                    metricDb.insert_version(projectId, revision, developer, message, commit_date, sprint_id);
                    
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

    @Override
    public String getImportName() {
        return "metric versions";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_metric_versions.json"};
    }
}
