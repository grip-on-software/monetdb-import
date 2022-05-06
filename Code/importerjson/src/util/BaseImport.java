/**
 * Import task runner.
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
package util;
import java.io.File;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 * Abstract base class for an importer task runner.
 * @author Enrique
 */
public abstract class BaseImport extends BaseDb {
    private String projectName;
    private int projectID = 0;
    private Set<String> problematicImports = new TreeSet<>();
    
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
    
    public final File getExportPath() {
        return getProjectPath(getProjectName());
    }

    /**
     * Get the path to the main file that is imported.
     * @return File path
     */
    public String getMainImportPath() {
        File path = new File(getExportPath(), getImportFiles()[0]);
        return path.toString();
    }
    
    /**
     * Get a collection of problematic file names which could not be imported by
     * this or earlier imports.
     * @return Problematic files 
     */
    public Set<String> getProblematicImports() {
        return problematicImports;
    }

    /**
     * Set a collection of problematic file names which could not be imported by
     * earlier imports.
     * @param problematicImports Problematic files 
     */
    public void setProblematicImports(Collection<String> problematicImports) {
        this.problematicImports.addAll(problematicImports);
    }
}
