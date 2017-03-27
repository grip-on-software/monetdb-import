/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

/**
 *
 * @author Enrique
 */
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
    
    /**
     * Perform the import of the JSON data for this task.
     */
    public abstract void parser();
    
    /**
     * Get the type of data that this importer processes.
     * @return the importer name 
     */
    public abstract String getImportName();
}
