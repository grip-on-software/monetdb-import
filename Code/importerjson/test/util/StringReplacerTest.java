/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
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
        System.out.println("execute");
        String text = "Test: aa bb cc dd";
        StringReplacer instance = new StringReplacer();
        instance.add("aa", "bbb").add("bb", "ccc");
        String expResult = "Test: bbb ccc cc dd";
        String result = instance.execute(text);
        assertEquals(expResult, result);
    }
    
}
