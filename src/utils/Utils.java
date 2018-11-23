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
}
