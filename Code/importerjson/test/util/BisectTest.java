/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 *
 * @author Leon Helwerda
 */
@RunWith(Parameterized.class)
public class BisectTest {
    Object[] array; 
    Object input;
    int rightExpected;
    int leftExpected;
    
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Object[] dates = new Object[]{
            Timestamp.valueOf("2017-01-01 10:00:00"),
            Timestamp.valueOf("2017-02-01 10:00:00"),
            Timestamp.valueOf("2017-03-01 10:00:00"),
            Timestamp.valueOf("2017-04-01 10:00:00")
        };
        return Arrays.asList(new Object[][] {
            { dates, Timestamp.valueOf("2016-12-31 12:34:56"), 0, 0 },
            { dates, Timestamp.valueOf("2017-02-02 10:10:10"), 2, 2 },
            { dates, Timestamp.valueOf("2017-02-01 10:00:00"), 2, 1 },
            { dates, Timestamp.valueOf("2017-04-04 10:20:30"), 4, 4 }
        });
    }
    
    public BisectTest(Object[] array, Object input, int rightExpected, int leftExpected) {
        this.array = array;
        this.input = input;
        this.rightExpected = rightExpected;
        this.leftExpected = leftExpected;
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
     * Test of bisectRight method, of class Bisect.
     */
    @Test
    public void testBisectRight() {
        int result = Bisect.bisectRight(array, input);
        assertEquals(rightExpected, result);
    }
    
    /**
     * Test of bisectRight method, of class Bisect.
     */
    @Test
    public void testBisectLeft() {
        int result = Bisect.bisectLeft(array, input);
        assertEquals(leftExpected, result);
    }
    
}
