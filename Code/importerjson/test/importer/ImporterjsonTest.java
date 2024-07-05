/**
 * Importer integration test.
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

import dao.DataSource;
import java.beans.PropertyVetoException;
import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

/**
 * Tests for the Importerjson class.
 * @author Leon Helwerda
 */
@RunWith(Parameterized.class)
public class ImporterjsonTest {
    private Properties props;
    private final String project;
    private DataSource datasource;
    
    @Parameterized.Parameters
    public static List<String> data() {
        return Arrays.asList(new String[]{"TEST1", "TEST2", "TEST3", "TEST4", "TEST5", "TEST6", "TEST7", "TEST8", "TEST9", "TEST10"});
    }

    public ImporterjsonTest(String project) {
        this.project = project;
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        props = System.getProperties();
        // Set the database to a local test instance with gros_test DB name
        System.setProperty("importer.url", "jdbc:monetdb://localhost/gros_test");
        System.setProperty("importer.relPath", "export");
        System.setProperty("importer.log", "SEVERE");
        System.setProperty("importer.update", "metric_defaults_update.json metric_options_update.json project_meta_update.json project_sources_update.json seats_files.json tfs_update.json github_update.json gitlab_update.json latest_vcs_versions.json");
        try {
            datasource = DataSource.getInstance();
        } catch (PropertyVetoException ex) {
            Logger.getLogger(ImporterjsonTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @After
    public void tearDown() {
        System.setProperties(props);
        DataSource.reset();
    }
    
    /**
     * Test of formatUsage method of Importerjson class.
     */
    @Test
    public void testFormatUsage() {
        String shortUsage = Importerjson.formatUsage();
        assertTrue(shortUsage.startsWith("\nUsage:"));
        assertTrue(shortUsage.contains("importerjson.jar"));
        assertTrue(shortUsage.split("\n").length <= 4);
        
        String longUsage = Importerjson.formatUsage(true);
        assertTrue(longUsage.startsWith(shortUsage));
        assertTrue(longUsage.contains("-Dimporter.log"));
        assertTrue(longUsage.contains("- vcs: "));
    }
    
    /**
     * Test of selectAndPerformTasks method of Importerjson class.
     */
    @Test
    public void testSelectAndPerformTasks() {
        // Assumptions (which cause the test to be skipped if they fail) that check for database and test file existence.
        File exportPath = datasource.getProjectPath(project);
        Assume.assumeTrue("Export test files for project exist", exportPath.isDirectory());
        try {
            datasource.getConnection();
        } catch (SQLException ex) {
            Assume.assumeTrue("Database connection is not ready", false);
        }
        Importerjson.selectAndPerformTasks(new String[]{project});
        // Test special tasks (and selection/subtraction of grouped tasks)
        Importerjson.selectAndPerformTasks(new String[]{project, "vcs,-all,sprintlink,developerproject,metric_domain_name,metric_default_target,repo_source,issue_changelog_id,encrypt"});
    }
    
}
