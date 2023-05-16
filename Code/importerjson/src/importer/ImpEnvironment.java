/**
 * Source environment importer.
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

import dao.EnvironmentDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
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
                    try {
                        URL parsedUrl = new URL(url);
                        URL cleanUrl = new URL(parsedUrl.getProtocol(), parsedUrl.getHost(), parsedUrl.getPort(), parsedUrl.getFile());
                        url = cleanUrl.toString();
                    }
                    catch (MalformedURLException ex) {
                        getLogger().log(Level.INFO, "Source environment {0} is not a URL: {1}", new Object[]{url, ex.getMessage()});
                    }
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
