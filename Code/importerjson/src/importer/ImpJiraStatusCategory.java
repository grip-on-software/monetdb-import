/*
 * Copyright 2017-2018 ICTU
 * 
 * Importer of gathered software development metrics for MonetDB
 */
package importer;

import dao.StatusDb;
import java.io.FileReader;
import org.json.simple.JSONObject;
import util.BaseImport;
import util.BufferedJSONReader;

/**
 * Importer for JIRA status categories.
 * @author Leon Helwerda
 */
public class ImpJiraStatusCategory extends BaseImport {

    @Override
    public void parser() {
        try (
            StatusDb statusDb = new StatusDb();
            FileReader fr = new FileReader(getMainImportPath());
            BufferedJSONReader br = new BufferedJSONReader(fr)
        ) {
            Object o;
            while ((o = br.readObject()) != null) {
                JSONObject jsonObject = (JSONObject) o;
                
                Long id = (Long) jsonObject.get("id");
                String key = (String) jsonObject.get("key");
                String name = (String) jsonObject.get("name");
                String color = (String) jsonObject.get("color");
                
                int category_id = id.intValue();
                
                StatusDb.CheckResult result = statusDb.check_category(category_id, key, name, color);
                if (result == StatusDb.CheckResult.MISSING) {
                    statusDb.insert_category(category_id, key, name, color);
                }
                else if (result == StatusDb.CheckResult.DIFFERS) {
                    statusDb.update_category(category_id, key, name, color);
                }
            }
        }
        catch (Exception ex) {
            logException(ex);
        }        
    }

    @Override
    public String getImportName() {
        return "JIRA status categories";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_status_category.json"};
    }
    
}
