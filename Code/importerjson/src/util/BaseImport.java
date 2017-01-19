/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

/**
 *
 * @author Enrique
 */

import dao.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;


public abstract class BaseImport extends BaseDb {
    
    private String projectName;
    private int projectID = 0;
    
    /**
     * @return the projectID
     */
    public final int getProjectID() {
        return projectID;
    }

    /**
     * @param projectID the projectID to set
     */
    public final void setProjectID(int projectID) {
        this.projectID = projectID;
    }
    
    /**
     * @return the project name
     */
    public final String getProjectName() {
        return projectName;
    }
    
    /**
     * @param projectName the project name to set
     */
    public final void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    public abstract void parser();
}
