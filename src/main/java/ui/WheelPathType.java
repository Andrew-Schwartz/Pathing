package ui;

public enum WheelPathType {
    NONE("NONE"),
    PATH("Perpendicular to path"),
    VEL("Lengths and dir according to vel");

    String type;

    WheelPathType(String type) {
        this.type = type;
    }
}
