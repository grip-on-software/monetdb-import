/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package importerjson;

/**
 *
 * @author Enrique
 */

import java.util.ResourceBundle;


public abstract class BaseImport {
    
    protected String url;
    protected String user;
    protected String password;
    protected String path;
    protected String project;
    
    public BaseImport() {
        ResourceBundle bundle = ResourceBundle.getBundle("importerjson.import");
        url = bundle.getString("url").trim();
        user = bundle.getString("user").trim();
        password = bundle.getString("password").trim();
        path = bundle.getString("path").trim();
        project = bundle.getString("project").trim();        
    }
}
