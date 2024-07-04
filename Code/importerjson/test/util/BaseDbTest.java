/**
 * Database access management test.
 * 
 * Copyright 2017-2020 ICTU
 * Copyright 2017-2022 Leiden University
 * Copyright 2017-2023 Leon Helwerda
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
import java.nio.file.Path;
import java.util.Properties;
import java.util.ResourceBundle;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for BaseDb methods.
 * @author Leon Helwerda
 */
public class BaseDbTest {
    private Properties props;
    
    public BaseDbTest() {
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
    }
    
    @After
    public void tearDown() {
        System.setProperties(props);
    }

    /**
     * Test of getBundle method, of class BaseDb.
     */
    @Test
    public void testGetBundle() {
        BaseDb instance = new BaseDb();
        ResourceBundle expResult = ResourceBundle.getBundle("util.import");
        ResourceBundle result = instance.getBundle();
        assertEquals(expResult, result);
    }

    /**
     * Test of getUrl method, of class BaseDb.
     */
    @Test
    public void testGetUrl() {
        System.setProperty("importer.url", "test://url");
        BaseDb instance = new BaseDb();
        String expResult = "test://url";
        String result = instance.getUrl();
        assertEquals(expResult, result);
    }

    /**
     * Test of setUrl method, of class BaseDb.
     */
    @Test
    public void testSetUrl() {
        String url = "new://url";
        BaseDb instance = new BaseDb();
        instance.setUrl(url);
        assertEquals(url, instance.getUrl());
    }

    /**
     * Test of getUser method, of class BaseDb.
     */
    @Test
    public void testGetUser() {
        System.setProperty("importer.user", "testuser");
        BaseDb instance = new BaseDb();
        String expResult = "testuser";
        String result = instance.getUser();
        assertEquals(expResult, result);
    }

    /**
     * Test of setUser method, of class BaseDb.
     */
    @Test
    public void testSetUser() {
        String user = "newuser";
        BaseDb instance = new BaseDb();
        instance.setUser(user);
        assertEquals(user, instance.getUser());
    }

    /**
     * Test of getPassword method, of class BaseDb.
     */
    @Test
    public void testGetPassword() {
        System.setProperty("importer.password", "testpass");
        BaseDb instance = new BaseDb();
        String expResult = "testpass";
        String result = instance.getPassword();
        assertEquals(expResult, result);
    }

    /**
     * Test of setPassword method, of class BaseDb.
     */
    @Test
    public void testSetPassword() {
        String password = "newpass";
        BaseDb instance = new BaseDb();
        instance.setPassword(password);
        assertEquals(password, instance.getPassword());
    }

    /**
     * Test of getRootPath method, of class BaseDb.
     */
    @Test
    public void testGetRootPath() {
        System.setProperty("user.dir", "/test/root/path");
        BaseDb instance = new BaseDb();
        File expResult = new File("/test/root/path/");
        Path result = instance.getRootPath();
        assertEquals(expResult.toPath(), result);
    }

    /**
     * Test of getPath method, of class BaseDb.
     */
    @Test
    public void testGetPath() {
        System.setProperty("user.dir", "/test/root/path");
        System.setProperty("importer.relPath", "test/rel/path");
        BaseDb instance = new BaseDb();
        File expResult = new File("/test/root/path/test/rel/path/");
        Path result = instance.getPath();
        assertEquals(expResult.toPath(), result);
    }

    /**
     * Test of setPath method, of class BaseDb.
     */
    @Test
    public void testSetPath() {
        String path = "/test/new/path";
        File expResult = new File(path);
        BaseDb instance = new BaseDb();
        instance.setPath(path);
        assertEquals(expResult.toPath(), instance.getPath());
    }
    
}
