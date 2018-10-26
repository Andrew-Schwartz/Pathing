package ui;

public enum PointMenuResult {
    DELETE_POINT("Delete This Point"),
    POINT_EDIT_MODE("Point Moving Mode"),
    TOGGLE_OVERRIDE_VEL("(WIP) Toggle Overriding Max Vel"); //TODO maybe NOT a checkbox in pointRow

    String result;

    PointMenuResult(String result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return result;
    }
}