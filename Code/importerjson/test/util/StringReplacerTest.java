/**
 * String replacement test.
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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for StringReplacer methods.
 * @author leonhelwerda
 */
public class StringReplacerTest {
    
    public StringReplacerTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of add method, of class StringReplacer.
     */
    @Test
    public void testAdd() {
        String search = "aa";
        String replace = "bbb";
        StringReplacer instance = new StringReplacer();
        assertEquals(instance, instance.add(search, replace));
    }
    
    /**
     * Test of isElongating method, of class StringReplacer.
     */
    @Test
    public void testIsElongating() {
        StringReplacer instance = new StringReplacer();
        assertFalse(instance.isElongating());
        instance.add("aa", "bbb");
        assertTrue(instance.isElongating());
    }

    /**
     * Test of execute method, of class StringReplacer.
     */
    @Test
    public void testExecute() {
        String text = "Test: aa bb cc dd";
        StringReplacer instance = new StringReplacer();
        instance.add("aa", "bbb").add("bb", "ccc");
        String expResult = "Test: bbb ccc cc dd";
        String result = instance.execute(text);
        assertEquals(expResult, result);
    }
    
}
