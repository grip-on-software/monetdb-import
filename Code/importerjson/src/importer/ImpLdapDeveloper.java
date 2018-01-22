/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.DeveloperDb;
import dao.DeveloperDb.Developer;
import dao.SaltDb;
import java.io.FileReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.BaseImport;

/**
 * Importer for JIRA developers.
 * @author Thomas and Enrique
 */
public class ImpLdapDeveloper extends BaseImport {
    
    @Override
    public void parser(){
        JSONParser parser = new JSONParser();
        int project_id = this.getProjectID();
 
        try (
            DeveloperDb devDb = new DeveloperDb();
            FileReader fr = new FileReader(getMainImportPath())
        ) {
            JSONArray a = (JSONArray) parser.parse(fr);
            
            for (Object o : a) {
                JSONObject jsonObject = (JSONObject) o;
                
                String display_name = (String) jsonObject.get("display_name");
                String name = (String) jsonObject.get("name");
                String email = (String) jsonObject.get("email");
                
                Developer dev = new Developer(name, display_name, email);
                Integer jira_id = devDb.check_ldap_developer(project_id, dev, SaltDb.Encryption.NONE);
                // check whether the developer does not already exist
                if (jira_id == null) {
                    jira_id = devDb.check_developer(dev);
                    devDb.insert_ldap_developer(project_id, jira_id, dev);
                }
            }                  
        }
        catch (Exception ex) {
            logException(ex);
        }
        
    }

    @Override
    public String getImportName() {
        return "LDAP developers";
    }

    @Override
    public String[] getImportFiles() {
        return new String[]{"data_ldap.json"};
    }

}