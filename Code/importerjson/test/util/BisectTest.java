/**
 * Bisection search test.
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
 * Unit tests for Bisect methods.
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
