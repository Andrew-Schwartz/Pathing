//package utils;
//
//import ui.UIController;
//
//import static utils.Config.ticksPerRev;
//import static utils.Config.wheelRadius;
//
//public class UnitConverter {
//    private static final int FIELD_WIDTH_INCHES = 324;
////    private static final int FIELD_LENGTH_INCHES = 384;
//
//    public static double inchesToPixels(double dist) {
//        return dist * UIController.imageWidth() / FIELD_WIDTH_INCHES;
//    }
//
//    public static double feetToPixels(double dist) {
//        return inchesToPixels(feetToInches(dist));
//    }
//
//    public static double feetToInches(double dist) {
//        return 12 * dist;
//    }
//
//    public static double inchesToFeet(double dist) {
//        return dist / 12.;
//    }
//
//    public static double pixelsToInches(double dist) {
//        return dist * FIELD_WIDTH_INCHES / UIController.imageWidth();
//    }
//
//    public static double pixelsToFeet(double dist) {
//        return inchesToFeet(pixelsToInches(dist));
//    }
//
//    /**
//     * @param rotationalVel in revs/sec
//     * @return linear velocity in (inches)/sec
//     */
//    public static double rotationalToLinear(double rotationalVel) {
//        return rotationalVel * wheelRadius() * Math.PI * 2;
//    }
//
//    /**
//     * @param linearVel in inches/sec
//     * @return rotational velocity in revs/sec
//     */
//    public static double linearToRotational(double linearVel) {
//        return linearVel / (wheelRadius() * Math.PI * 2);
//    }
//
//    /**
//     * @param seconds denominator in seconds
//     * @return denominator in minutes
//     */
//    public static double secToMin(double seconds) {
//        return seconds / 60.0;
//    }
//
//    /**
//     * we love CTRE
//     */
//    public static double secTo100Ms(double seconds) {
//        return seconds / 10.0;
//    }
//
//    /**
//     * @param minutes denominator in minutes
//     * @return denominator in seconds
//     */
//    public static double minToSec(double minutes) {
//        return minutes * 60.0;
//    }
//
//    /**
//     * @return number of rotations made from moving some number of ticks
//     */
//    public static double ticksToRotations(double ticks) {
//        return ticks / ticksPerRev();
//    }
//
//    /**
//     * @return number of ticks to do some number of rotations
//     */
//    public static double rotationsToTicks(double rotations) {
//        return rotations * ticksPerRev();
//    }
//
//    /**
//     * @param angle   angle to be rotated, in radians
//     * @param degrees number of degrees to rotate angle, in degrees. CCW is positive
//     * @return rotated angle, in radians
//     */
//    public static double rotateAngle(double angle, double degrees) {
//        double x = Math.cos(angle),
//                y = Math.sin(angle);
//        degrees = Math.toRadians(degrees);
//        double newX = x * Math.cos(degrees) - y * Math.sin(degrees),
//                newY = y * Math.cos(degrees) + x * Math.sin(degrees);
//        return Math.atan2(newY, newX);
//    }
//
//    /**
//     * rotates an angle from robot centric (forwards is 0, left is negative pi/2) to x-axis based (x=1 y=0 is 0, x=0 y=1 is pi/2)
//     *
//     * @param angle angle to be rotated in radians
//     * @return rotated angle in radians
//     */
//    public static double rotateRobotToCartesian(double angle) {
//        return rotateAngle(-angle, 90);
//    }
//}