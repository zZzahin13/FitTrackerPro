package com.example.fittrackerpro.utils;

public class NumberUtil {

    // 1.0 -> "1", 1.5 -> "1.5"
    public static String trim(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}