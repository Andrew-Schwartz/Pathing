package utils;

import ui.UIController;

public class UnitConverter {
    private static final int FIELD_WIDTH_INCHES = 324;
//    private static final int FIELD_LENGTH_INCHES = 384;

    public static double inchesToPixels(double dist) {
        return dist * UIController.imageWidth() / FIELD_WIDTH_INCHES;
    }

    public static double feetToPixels(double dist) {
        return inchesToPixels(dist*12);
    }

    /**
     *
     * @param angle angle to be rotated, in radians
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

//    public static double inchesToScreenVert(double dist) {
//        dist = UIController.imageHeight() - dist;
//        return dist * UIController.imageHeight() / FIELD_LENGTH_INCHES;
//    }
}