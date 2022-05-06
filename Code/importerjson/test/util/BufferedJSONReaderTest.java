/**
 * Buffered reader for JSON file test.
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

import java.io.EOFException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Unit tests for BufferedJSONReader methods.
 * @author leonhelwerda
 */
@RunWith(Parameterized.class)
public class BufferedJSONReaderTest {
    String jsonInput;
    Object expected;
    
    @Parameters
    public static Collection<Object[]> data() {
        HashMap<String, Object> map1 = new HashMap<>();
        map1.put("foo", 1);
        map1.put("bar", 2);
        
        Object result1 = new JSONObject(map1);
        return Arrays.asList(new Object[][] {
            { "", null },
            { "[]", null },
            { "\"some string contents\"", "some string contents" },
            { "[\n{\n\"foo\": 1,\n\"bar\": 2\n}\n]", result1 },
            { "\n", EOFException.class },
            { "[", EOFException.class },
            { "[\n{\n\"foo\": 1,\n\"bar\": 2\n},\n{\"foo\": 3,\n\"bar\": 4}\n]", result1 }
        });   
    }
    
    @Rule
    public final ExpectedException exception = ExpectedException.none();
  
    public BufferedJSONReaderTest(String jsonInput, Object expected) {
        this.jsonInput = jsonInput;
        this.expected = expected;
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
     * Test of readObject method, of class BufferedJSONReader.
     * @throws java.lang.Exception
     */
    @Test
    public void testReadObject() throws Exception {
        try (BufferedJSONReader instance = new BufferedJSONReader(new StringReader(jsonInput))) {
            if (expected instanceof Class && Throwable.class.isAssignableFrom((Class)expected)) {
                Class<?> expectedClass = (Class<?>)expected;
                exception.expect(expectedClass.asSubclass(Throwable.class));
            }
            Object result = instance.readObject();
            if (expected instanceof JSONObject) {
                assertEquals(expected.toString(), result.toString());
            }
            else {
                assertEquals(expected, result);
            }
        }
    }
    
}
