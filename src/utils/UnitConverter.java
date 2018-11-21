package utils;

import ui.UIController;

public class UnitConverter {
    private static final int FIELD_WIDTH_INCHES = 324;
//    private static final int FIELD_LENGTH_INCHES = 384;

    public static double inchesToPixels(double dist) {
        return dist * UIController.imageWidth() / FIELD_WIDTH_INCHES;
    }

    public static double feetToPixels(double dist) {
        return inchesToPixels(feetToInches(dist));
    }

    public static double feetToInches(double dist) {
        return 12 * dist;
    }

    public static double inchesToFeet(double dist) {
        return dist / 12.;
    }

    public static double pixelsToInches(double dist) {
        return dist * FIELD_WIDTH_INCHES / UIController.imageWidth();
    }

    public static double pixelsToFeet(double dist) {
        return inchesToFeet(pixelsToInches(dist));
    }

    /**
     * @param rotationalVel in rotations/sec
     * @return linear velocity in (inches)/sec
     */
    public static double rotationalToLinear(double rotationalVel) {
        return rotationalVel * Config.getDoubleProperty("wheel_radius") * Math.PI * 2;
    }

    /**
     * @param linearVel in inches/sec
     * @return rotational velocity in rotations/sec
     */
    public static double linearToRotational(double linearVel) {
        return linearVel / (Config.getDoubleProperty("wheel_radius") * Math.PI * 2);
    }

    /**
     * @param angle   angle to be rotated, in radians
     * @param degrees number of degrees to rotate angle, in degrees. CCW is positive
     * @return rotated angle, in radians
     */
    public static double rotateAngle(double angle, double degrees) {
        double x = Math.cos(angle),
                y = Math.sin(angle);
        degrees = Math.toRadians(degrees);
        double newX = x * Math.cos(degrees) - y * Math.sin(degrees),
                newY = y * Math.cos(degrees) + x * Math.sin(degrees);
        return Math.atan2(newX, newY);
    }

    /**
     * rotates an angle from robot centric (forwards is 0, left is negative pi/2) to x-axis based (x=1 y=0 is 0, x=0 y=1 is pi/2)
     *
     * @param angle angle to be rotated in radians
     * @return rotated angle in radians
     */
    public static double rotateRobotToCartesian(double angle) {
        return rotateAngle(-angle, -90);
    }
}