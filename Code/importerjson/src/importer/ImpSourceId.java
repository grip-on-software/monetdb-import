/**
 * Source ID importer.
 * 
 * Copyright 2017-2020 ICTU
 * Copyright 2017-2022 Leiden University
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

import dao.MetricDb;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 * Importer for source identifiers.
 * @author Leon Helwerda
 */
public class ImpSourceId extends BaseImport {

    @Override
    public void parser() {
        JSONParser parser = new JSONParser();
        int project = getProjectID();
         
        try (
            FileReader fr = new FileReader(getMainImportPath());
            MetricDb metricDb = new MetricDb()
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                String domain_name = (String) jsonObject.get("domain_name");
                String url = (String) jsonObject.get("url");
                String source_id = (String) jsonObject.get("source_id");
                String source_type = (String) jsonObject.get("source_type");
                String domain_type = (String) jsonObject.get("domain_type");
                if (url != null) {
                    MetricDb.CheckResult result = metricDb.check_source_id(project, domain_name, url, source_type, source_id, domain_type);
                    if (result == MetricDb.CheckResult.MISSING) {
                        metricDb.insert_source_id(project, domain_name, url, source_type, source_id, domain_type);
                    }
                    else if (result == MetricDb.CheckResult.DIFFERS) {
                        metricDb.update_source_id(project, domain_name, url, source_type, source_id, domain_type);
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
        return "source identifiers";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_source_ids.json"};
    }
    
}
