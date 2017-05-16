/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
}