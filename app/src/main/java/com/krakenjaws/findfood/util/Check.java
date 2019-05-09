package com.krakenjaws.findfood.util;

/**
 * Created by Andrew on 5/8/2019.
 */
public class Check {

    /**
     * Return true if the @param is null
     *
     * @param string String to check
     * @return True if null, False if not
     */
    public static boolean isEmpty(String string) {
        return string.equals("");
    }

    /**
     * Return true if @param 's1' matches @param 's2'
     *
     * @param s1 First string
     * @param s2 Second string
     * @return True or False if they match or not
     */
    public static boolean doStringsMatch(String s1, String s2) {
        return s1.equals(s2);
    }
}
