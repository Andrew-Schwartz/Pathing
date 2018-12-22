package ui;

import java.util.Arrays;

public enum PointMenuResult {
    MENU("Display Menu"),
    DELETE_POINT("Delete This Point"),
    POINT_EDIT_MODE("Point Moving Mode"),
    TOGGLE_OVERRIDE_VEL("Toggle Overriding Max Vel"),
    TOGGLE_BACKWARDS("Toggle Backwards Mode");

    String result;

    PointMenuResult(String result) {
        this.result = result;
    }

    public static String[] valueStrings() {
        return Arrays.stream(values())
                .map(PointMenuResult::toString)
                .toArray(String[]::new);
//        var vals = PointMenuResult.values();
//        var stringVals = new String[vals.length];
//        for (int i = 0; i < vals.length; i++) {
//            stringVals[i] = vals[i].toString();
//        }
//        return stringVals;
    }

    @Override
    public String toString() {
        return result;
    }
}