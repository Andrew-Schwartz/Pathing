package ui;

public enum PointMenuResult {
    DELETE_POINT("Delete This Point"),
    POINT_EDIT_MODE("Point Moving Mode"),
    TOGGLE_OVERRIDE_VEL("(WIP) Toggle Overriding Max Vel"); //TODO maybe NOT a checkbox in pointRow

    String result;

    PointMenuResult(String result) {
        this.result = result;
    }

    public static String[] valueStrings() {
        PointMenuResult[] vals = PointMenuResult.values();
        String[] stringVals = new String[vals.length];
        for (int i = 0; i < vals.length; i++) {
            stringVals[i] = vals[i].toString();
        }
        return stringVals;
    }

    @Override
    public String toString() {
        return result;
    }
}