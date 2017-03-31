/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package importer;

import dao.UpdateDb;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.BaseImport;

/**
 *
 * @author Leon Helwerda
 */
public class ImpUpdateTracker extends BaseImport {

    @Override
    public void parser() {
        int project_id = getProjectID();
        String updateFiles = System.getProperty("importer.update", "");
        String[] updateNames = updateFiles.trim().split(" ");
        try (UpdateDb updateDb = new UpdateDb()) {
            for (String updateFilename : updateNames) {
                try {
                    String pathName = getPath()+getProjectName()+updateFilename;
                    Path path = Paths.get(pathName);
                    File file = new File(pathName);
                    Timestamp update_date = new Timestamp(file.lastModified());
                    String contents = new String(Files.readAllBytes(path), "UTF-8");
                    if (!updateDb.check_file(project_id, updateFilename)) {
                        updateDb.insert_file(project_id, updateFilename, contents, update_date);
                    }
                    else {
                        updateDb.update_file(project_id, updateFilename, contents, update_date);
                    }
                }
                catch (IOException ex) {
                    Logger.getLogger(ImpUpdateTracker.class.getName()).log(Level.WARNING, "Cannot import update tracking file " + updateFilename, ex);
                }
            }
        }
        catch (Exception ex) {
            logException(ex);
        }
    }

    @Override
    public String getImportName() {
        return "update tracking files";
    }
    
}