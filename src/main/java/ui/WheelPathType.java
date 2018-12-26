package ui;

public enum WheelPathType {
    NONE("NONE"),
    PATH("Perpendicular to path"),
    VEL("Length and dir according to vel");

    String type;

    WheelPathType(String type) {
        this.type = type;
    }
}
