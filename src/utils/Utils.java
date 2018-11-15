package utils;

public class Utils {
    public static double parseDouble(String s, double defaultVal) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    public static double parseDouble(String s) {
        return parseDouble(s, 0);
    }
}
