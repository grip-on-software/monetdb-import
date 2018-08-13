/*
 * Copyright 2017-2018 ICTU
 * 
 * Importer of gathered software development metrics for MonetDB
 */
package importer;

import dao.EnvironmentDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 * Importer for source environments.
 * @author Leon Helwerda
 */
public class ImpEnvironment extends BaseImport {

    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        int project = getProjectID();
         
        try (
            FileReader fr = new FileReader(getMainImportPath());
            EnvironmentDb envDb = new EnvironmentDb()
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                String environment = jsonObject.get("environment").toString();
                String type = (String) jsonObject.get("type");
                String url = (String) jsonObject.get("url");
                String version = (String) jsonObject.get("version");
                if (url != null) {
                    EnvironmentDb.CheckResult result = envDb.check_source(project, type, url, environment, version);
                    if (result == EnvironmentDb.CheckResult.MISSING) {
                        envDb.insert_source(project, type, url, environment, version);
                    }
                    else if (result == EnvironmentDb.CheckResult.DIFFERS) {
                        envDb.update_source(project, type, url, environment, version);
                    }
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
        return "source environments";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_environments.json"};
    }
    
}
