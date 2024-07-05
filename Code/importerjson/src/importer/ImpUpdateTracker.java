/**
 * Update tracker importer.
 * 
 * Copyright 2017-2020 ICTU
 * Copyright 2017-2022 Leiden University
 * Copyright 2017-2024 Leon Helwerda
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

import dao.UpdateDb;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;
import util.BaseImport;

/**
 * Importer for update trackers.
 * @author Leon Helwerda
 */
public class ImpUpdateTracker extends BaseImport {

    @Override
    public void parser() {
        try {
            if (!getProblematicImports().isEmpty()) {
                throw new ImporterException("Cannot import update trackers: earlier imports had problems");
            }
        } catch (RuntimeException ex) {
            logException(ex);
            return;
        }
        
        int project_id = getProjectID();
        String[] updateNames = getImportFiles();
        try (UpdateDb updateDb = new UpdateDb()) {
            for (String updateFilename : updateNames) {
                addUpdateTracker(updateFilename, updateDb, project_id);
            }
        }
        catch (Exception ex) {
            logException(ex);
        }
    }

    private void addUpdateTracker(String updateFilename, final UpdateDb updateDb, int project_id) throws PropertyVetoException, SQLException {
        try {
            File file = new File(getExportPath(), updateFilename);
            Timestamp update_date = new Timestamp(file.lastModified());
            String contents = new String(Files.readAllBytes(file.toPath()), "UTF-8");
            if (!updateDb.check_file(project_id, updateFilename)) {
                updateDb.insert_file(project_id, updateFilename, contents, update_date);
            }
            else {
                updateDb.update_file(project_id, updateFilename, contents, update_date);
            }
        }
        catch (IOException ex) {
            getLogger().log(Level.WARNING, "Cannot import update tracking file {0}: {1}", new Object[]{updateFilename, ex.getMessage()});
        }
    }

    @Override
    public String getImportName() {
        return "update tracking files";
    }

    @Override
    public String[] getImportFiles() {
        String updateFiles = System.getProperty("importer.update", "").trim();
        if (updateFiles.isEmpty()) {
            getLogger().log(Level.WARNING, "No update tracker files specified");
            return new String[]{};
        }
        return updateFiles.split(" ");
    }
    
}
