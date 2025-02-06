package com.txtnet.txtnetbrowser.util;

import android.util.Log;

public class CRC5 {
    // CRC-7 Polynomial from https://users.ece.cmu.edu/~koopman/crc/crc5.html 0x12 = x^5 +x^2 +1 (0x12; 0x25) <=> (0x14; 0x29)
    private static final int POLYNOMIAL = 0x12;
    private static final int CRC_INITIAL = 0x00; // Either 0x00 or 0x1F?

    private static final int MAX_V = 406; // Max value for V
    private static final int CRC_BITS = 32; // 5-bit CRC (0-31)

    public static int computeCRC5(byte[] data) {
        int crc = CRC_INITIAL; // Start with initial CRC value

        for (byte b : data) {
            crc ^= (b & 0xFF); // XOR input byte into the CRC register

            for (int i = 0; i < 8; i++) { // Process each bit
                if ((crc & 0x10) != 0) { // If bit 4 (since it's a 5-bit CRC) is set
                    crc = ((crc << 1) ^ POLYNOMIAL) & 0x1F; // Apply polynomial and mask to 5 bits
                } else {
                    crc = (crc << 1) & 0x1F; // Just shift left and mask to 5 bits
                }
            }
        }

        return crc; // 7-bit result
    }

    // With a CRC5 and a 114^2 combination counter, data can be split up across up to 406 text messages.
    // This is a far cry from the originally supported 12996 messages per transaction, but I believe the CRC will be far more useful in the real world.

    //Modulo arithematic: P=32V+C where C is 5-bit CRC (0-31). Then V≤⌊12995/32⌋=406
    // Technically wastes 3 possibilities since 32*406 = 12992, but close enough
    public static int packWithCRC5(int SMScounterValue, int crc) {
        if (SMScounterValue < 0 || SMScounterValue > MAX_V || crc < 0 || crc >= CRC_BITS) {
            throw new IllegalArgumentException("Value or CRC out of range");
        }
        return SMScounterValue * CRC_BITS + crc;
    }

    public static int[] unpackCRC5(int packedSMSCounterValue) {
        if (packedSMSCounterValue < 0 || packedSMSCounterValue > 12995) {
            throw new IllegalArgumentException("Packed value out of range");
        }
        int smsCounterValue = packedSMSCounterValue / CRC_BITS;
        int crc = packedSMSCounterValue % CRC_BITS;
        return new int[]{smsCounterValue, crc};
    }

//    Original implementation - does not work because max 12996 is not base 2-oriented - some CRC values would be >2 chars in base 114
//    public static int packWithCRC5(int SMScounterValue, int crc) {
//        if (SMScounterValue < 0 || SMScounterValue > 511) {
//            throw new IllegalArgumentException("Value must be between 0 and 511.");
//        }
//        if (crc < 0 || crc > 31) {
//            throw new IllegalArgumentException("CRC must be between 0 and 31.");
//        }
//
//        // Shift CRC left by 9 bits and OR with the value
//        return (crc << 9) | SMScounterValue;
//    }
//
//    public static int[] unpackCRC5(int packedSMSCounterValue) {
//        int value = packedSMSCounterValue & 0x1FF;  // Extract lower 9 bits (0x1FF = 9 bits set)
//        int crc = (packedSMSCounterValue >> 9) & 0x1F;  // Extract upper 5 bits (0x1F = 5 bits set)
//
//        return new int[]{value, crc};  // Return as an array
//    }
}

