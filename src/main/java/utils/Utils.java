package utils;

public class Utils {
    public static double parseDouble(String s, double defaultVal) {
        try {
            return Double.parseDouble(s);
        } catch (RuntimeException e) {
            return defaultVal;
        }
    }

    public static double parseDouble(String s) {
        return parseDouble(s, 0);
    }

    public static boolean parseBoolean(String s, boolean defaultVal) {
        try {
            return Boolean.parseBoolean(s);
        } catch (RuntimeException e) {
            return defaultVal;
        }
    }

    public static boolean parseBoolean(String s) {
        return parseBoolean(s, false);
    }

    public static String parseString(String s, String defaultVal) {
        if (s != null) return s;
        return defaultVal;
    }

    public static String parseString(String s) {
        return parseString(s, "");
    }

    public static double absRange(double d, double min, double max) {
        double sign = Math.signum(d);
        d = Math.abs(d);
        return sign * Math.max(Math.min(d, max), min);
    }

    public static boolean aboutEquals(double a, double b, double epsilon) {
        return a + epsilon >= b && a - epsilon <= b;
    }
}
