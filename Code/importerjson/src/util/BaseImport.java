/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

/**
 * Abstract base class for an importer task runner.
 * @author Enrique
 */
public abstract class BaseImport extends BaseDb {
    private String projectName;
    private int projectID = 0;
    
    /**
     * Retrieve the identifier of the project that the importer runs on.
     * Some specialized tasks may run globally, in which case the project ID is 0.
     * @return The project identifier
     */
    public final int getProjectID() {
        return projectID;
    }

    /**
     * Set the identifier of the project that the importer runs on.
     * @param projectID The project identifier to set
     */
    public final void setProjectID(int projectID) {
        this.projectID = projectID;
    }
    
    /**
     * Retrieve the shorthand name of the project that the importer runs on.
     * @return The project name
     */
    public final String getProjectName() {
        return projectName;
    }
    
    /**
     * Set the shorthand name of the project that the importer runs on. 
     * @param projectName The project name to set
     */
    public final void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    /**
     * Perform the import of the JSON data for the importer's task.
     */
    public abstract void parser();
    
    /**
     * Get the human-readale type of data that this importer processes.
     * @return The importer name, a noun phrase that can be used in status messages
     */
    public abstract String getImportName();
    
    /**
     * Get a list of file names that live in the gatherer's export directory
     * which are used during the import.
     * @return List of imported file names
     */
    public abstract String[] getImportFiles();
    
    public final String getExportPath() {
        return getPath() + getProjectName();
    }

    /**
     * Get the path to the main file that is imported.
     * @return File path
     */
    public String getMainImportPath() {
        return getExportPath() + "/" + getImportFiles()[0];
    }
}
