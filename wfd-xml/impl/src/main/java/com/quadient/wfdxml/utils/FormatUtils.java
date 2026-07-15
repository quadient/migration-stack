package com.quadient.wfdxml.utils;

public class FormatUtils {

    private FormatUtils() {
    }

    public static String formatPx(double value) {
        if (value == (long) value) {
            return (long) value + "px";
        }
        return value + "px";
    }
}
