/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
 *
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
            if (expected instanceof Class && Exception.class.isAssignableFrom((Class)expected)) {
                exception.expect((Class)expected);
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
