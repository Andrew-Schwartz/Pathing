package ui;

public enum PointMenuResult {
    NONE_SELECTED(""),
    DELETE_POINT("Delete This Point"),
    POINT_EDIT_MODE("Point Moving Mode"),
    REORDER_POINT("(Planned) Change Point Order");

    String result;

    PointMenuResult(String result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return result;
    }
}

