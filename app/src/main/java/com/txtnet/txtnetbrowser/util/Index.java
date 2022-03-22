package com.txtnet.txtnetbrowser.util;

public class Index {

    // Linear-search function to find the index of an element
    public static int findIndex(String arr[], String t) {

        // if array is Null
        if (arr == null) {
            return -1;
        }

        // traverse in the array
        for (int i = 0; i < arr.length; i++) {

            // if the i-th element is t
            // then return the index
            if (arr[i].equals(t)) {
                return i;
            }
        }
        return -1;
    }
}
