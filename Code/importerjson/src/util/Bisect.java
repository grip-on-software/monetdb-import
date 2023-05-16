/**
 * Bisection search.
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

import java.util.Arrays;

/**
 * Bisection utilities
 * @author Leon Helwerda
 */
public class Bisect {
    private Bisect() {}
    
    /**
     * Locate the index at which the key should be inserted into the array in
     * order to keep the sorted order of the array. If the key is equal to one
     * of the elements in the array, then the returned index is the one after
     * this element.
     * 
     * The array must be sorted into ascending order according to the natural
     * ordering of its elements prior to making this call. If it is not sorted,
     * the results are undefined. (If the array contains elements that are not
     * mutually comparable (for example, strings and integers), it cannot be
     * sorted according to the natural ordering of its elements, hence results
     * are undefined.) If the array contains multiple elements equal to the
     * specified object, there is no guarantee which index is returned.
     * @param array The array to be searched
     * @param key The value to be searched for
     * @return The index to insert at
     */
    public static int bisectRight(Object[] array, Object key) {
        return Math.abs(Arrays.binarySearch(array, key) + 1);
    }

    /**
     * Locate the index at which the key should be inserted into the array in
     * order to keep the sorted order of the specified range in the array.
     * If the key is equal to one of the elements in the range, then the
     * returned index is the one after this element.
     * 
     * The range must be sorted into ascending order according to the natural
     * ordering of its elements prior to making this call. If it is not sorted,
     * the results are undefined. (If the range contains elements that are not
     * mutually comparable (for example, strings and integers), it cannot be
     * sorted according to the natural ordering of its elements, hence results
     * are undefined.) If the range contains multiple elements equal to the
     * specified object, there is no guarantee which index is returned.
     * @param array The array to be searched
     * @param key The value to be searched for
     * @param from_index The index of the first element to consider in the
     * bisection search. The range includes this element.
     * @param to_index The index after the last element to consider in the
     * bisection search. The range excludes this element.
     * @return The index to insert at
     */
    public static int bisectRight(Object[] array, Object key, int from_index, int to_index) {
        return Math.abs(Arrays.binarySearch(array, from_index, to_index, key) + 1);
    }
    
    /**
     * Locate the index at which the key should be inserted into the array in
     * order to keep the sorted order of the array. If the key is equal to one
     * of the elements in the array, then the returned index corresponds to this
     * element's position in the array.
     * 
     * The array must be sorted into ascending order according to the natural
     * ordering of its elements prior to making this call. If it is not sorted,
     * the results are undefined. (If the array contains elements that are not
     * mutually comparable (for example, strings and integers), it cannot be
     * sorted according to the natural ordering of its elements, hence results
     * are undefined.) If the array contains multiple elements equal to the
     * specified object, there is no guarantee which index is returned.
     * @param array The array to be searched
     * @param key The value to be searched for
     * @return The index to insert at
     */
    public static int bisectLeft(Object[] array, Object key) {
        int idx = Arrays.binarySearch(array, key);
        return idx < 0 ? Math.abs(idx + 1) : idx;
    }
    
    /**
     * Locate the index at which the key should be inserted into the array in
     * order to keep the sorted order of the specified range in the array.
     * If the key is equal to one of the elements in the range, then the
     * returned index corresponds to this element's position in the array.
     * 
     * The range must be sorted into ascending order according to the natural
     * ordering of its elements prior to making this call. If it is not sorted,
     * the results are undefined. (If the range contains elements that are not
     * mutually comparable (for example, strings and integers), it cannot be
     * sorted according to the natural ordering of its elements, hence results
     * are undefined.) If the range contains multiple elements equal to the
     * specified object, there is no guarantee which index is returned.
     * @param array The array to be searched
     * @param key The value to be searched for
     * @param from_index The index of the first element to consider in the
     * bisection search. The range includes this element.
     * @param to_index The index after the last element to consider in the
     * bisection search. The range excludes this element.
     * @return The index to insert at
     */
    public static int bisectLeft(Object[] array, Object key, int from_index, int to_index) {
        int idx = Arrays.binarySearch(array, from_index, to_index, key);
        return idx < 0 ? Math.abs(idx + 1) : idx;
    }
}
