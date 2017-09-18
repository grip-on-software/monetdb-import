/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
                
                Object environment = jsonObject.get("environment");
                String type = (String) jsonObject.get("type");
                String url = (String) jsonObject.get("url");
                if (url != null && !envDb.check_source(project, environment.toString())) {
                    envDb.insert_source(project, type, url, environment.toString());
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
