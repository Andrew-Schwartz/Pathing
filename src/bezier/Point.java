package bezier;

import ui.UIController;
import utils.UnitConverter;
import utils.Utils;

public class Point {
    private double x, y;
    private boolean intercept;
    private double targetVelocity;

    private double leftPos, leftVel, rightPos, rightVel, heading;
    private boolean last = false;
    private boolean overrideMaxVel;

    private boolean empty = false; //allow blank point

    public Point(double x, double y, boolean intercept) {
//        x = Math.round(x * 10) / 10.0;
//        y = Math.round(y * 10) / 10.0;
        if (intercept) {
            this.x = Math.max(Math.min(x, UIController.imageWidth()), 0);
            this.y = Math.max(Math.min(y, UIController.imageHeight()), 0);
        } else {
            this.x = x;
            this.y = y;
        }
        this.intercept = intercept;
    }

    public Point(double x, double y) {
        this(x, y, false);
    }

    public Point(String x, String y, boolean intercept) {
        if (x.isEmpty() || y.isEmpty()) {
            empty = true;
        }
        this.x = Utils.parseDouble(x);
        this.y = Utils.parseDouble(y);

        this.intercept = intercept;
    }

    public double getX() {
        return x;
    }

    public boolean isIntercept() {
        return intercept;
    }

    public double getY() {
        return y;
    }

    public String getXString() {
        return empty ? "" : String.valueOf(x);
    }

    public String getYString() {
        return empty ? "" : String.valueOf(y);
    }

    public double distanceTo(Point p) {
        double a = p.getX() - getX(),
                b = p.getY() - getY();
        return Math.sqrt(a * a + b * b);
    }

    /**
     * Calculates angle from this point to a second point
     *
     * @param p point to find angle to
     * @return angle in degrees
     */
    public double angleTo(Point p) {
        double x = p.getX() - getX(),
                y = p.getY() - getY();
        double theta = Math.atan2(x, y);
        theta = Math.toDegrees(theta);
        return theta;
    }

    public void setHeading(double angleToNext) {
        heading = angleToNext;
    }

    /**
     * Direction of this point, in robot-centric orientation (-180 to 180)
     *
     * @return heading, in degrees
     */
    public double getHeading() {
        return heading;
    }

    /**
     * direction of this point, in x-axis based orientation (0 to 2pi)
     *
     * @return heading, in radians
     */
    public double getHeadingCartesian() {
        double angle = UnitConverter.rotateRobotToCartesian(Math.toRadians(getHeading()));
        if (angle < 180) angle = 360 - angle;
        return angle;
    }

    public void setLast(boolean isLast) {
        last = isLast;
    }

    public boolean isLast() {
        return last;
    }

    public void setIntercept(boolean intercept) {
        this.intercept = intercept;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    /**
     *
     * @param targetVelocity calculated velocity of this point in feet/second
     */
    public void setTargetVelocity(double targetVelocity) {
        this.targetVelocity = targetVelocity;
    }

    /**
     *
     * @return the calculated velocity of this point in feet/second
     */
    public double getTargetVelocity() {
        return targetVelocity;
    }

    /**
     * sets the angular velocities of left and right wheels
     * @param leftVel rotations/second
     * @param rightVel rotations/second
     */
    public void setVels(double leftVel, double rightVel) {
        this.leftVel = leftVel;
        this.rightVel = rightVel;
    }

    public double getLeftVel() {
        return leftVel;
    }

    public double getRightVel() {
        return rightVel;
    }

    public void setPos(double leftPos, double rightPos) {
        this.leftPos = leftPos;
        this.rightPos = rightPos;
    }

    /**
     * sets the target positions for this point based on this point's velocity
     * @param prevLeftPos left position of previous point
     * @param prevRightPos left position of previous point
     */
    public void advancePos(double prevLeftPos, double prevRightPos) {
        setPos(prevLeftPos + leftVel * 0.05, prevRightPos + rightVel * 0.05);
    }

    public double getLeftPos() {
        return leftPos;
    }

    public double getRightPos() {
        return rightPos;
    }

    public void overrideMaxVel(boolean overrideMaxVel) {
        this.overrideMaxVel = overrideMaxVel;
    }

    public void
    toggleOverride() {
        overrideMaxVel(!isOverrideMaxVel());
    }

    public boolean isOverrideMaxVel() {
        return overrideMaxVel;
    }

    @Override
    public String toString() {
        return "X=" + x + " ,Y=" + y + ", intercept=" + intercept;
    }
}
