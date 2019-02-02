package ui;

public enum PointMenuResult {
    NONE("none"),
    MENU("Display Menu"),
    DELETE_POINT("Delete This Point"),
    POINT_MOVE_MODE("Point Moving Mode"),
    TOGGLE_OVERRIDE_VEL("Toggle Overriding Max Vel"),
    TOGGLE_BACKWARDS("Toggle Backwards Mode"),
    MAX_VEL("Set to max vel reachable");

    String result;

    PointMenuResult(String result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return result;
    }
}
