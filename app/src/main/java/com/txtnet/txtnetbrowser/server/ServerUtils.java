package com.txtnet.txtnetbrowser.server;

import com.txtnet.txtnetbrowser.basest.Base10Conversions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ServerUtils {
    // Subset of 3GPP TS 23.038 Standard's Basic Character Set, as mobile carriers do not seem to 100% follow this standard
    public static String TS0338SafeString(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input must not be null");
        }

        // Marginal lookup speed improvement by using a HashSet
        Set<Integer> allowedCodePoints = new HashSet<Integer>();
        for (String s : Base10Conversions.SYMBOL_TABLE) {
            assert s.length() == 1;
            allowedCodePoints.add(s.codePointAt(0));
        }

        StringBuilder sb = new StringBuilder(input.length());

        int i = 0;
        while (i < input.length()) {
            int codePoint = input.codePointAt(i);
            sb.append(allowedCodePoints.contains(codePoint) ? Character.toChars(codePoint)[0] : "¿");

            // Move the index forward by the correct number of code units (handles surrogate pairs)
            i += Character.charCount(codePoint);
        }

        return sb.toString();
    }
}

