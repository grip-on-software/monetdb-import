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
            FileReader fr = new FileReader(getPath()+getProjectName()+"/data_environments.json");
            EnvironmentDb envDb = new EnvironmentDb()
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                Object environment = jsonObject.get("environment");
                String type = (String) jsonObject.get("type");
                String url = null;
                if (environment instanceof String) {
                    url = (String) environment;
                }
                else if (environment instanceof JSONArray) {
                    JSONArray environmentArray = (JSONArray) environment;
                    url = (String) environmentArray.get(0);
                }
                if (url != null) {
                    if (!envDb.check_source(project, url)) {
                        envDb.insert_source(project, type, url);
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
    
}
