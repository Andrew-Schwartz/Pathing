package bezier;

import ui.UIController;

public class Point {
    private double x, y;
    private boolean intercept;
    private double targetVelocity;

    private double leftPos, leftVel, rightPos, rightVel, angle;
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
        if (!x.isEmpty() && !y.isEmpty()) {
            this.x = Double.parseDouble(x);
            this.y = Double.parseDouble(y);
        } else {
            empty = true;
        }

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

    public double angleTo(Point p) {
        double x = p.getX() - getX(),
                y = p.getY() - getY();
        double theta = Math.atan2(x, y);
        theta = Math.toDegrees(theta);
        return theta;
    }

    public void setAngle(double angleToNext) {
        angle = angleToNext;
    }

    public double getAngle() {
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

    public void setTargetVelocity(double targetVelocity) {
        this.targetVelocity = targetVelocity;
    }

    public double getTargetVelocity() {
        return targetVelocity;
    }

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

    public double getLeftPos() {
        return leftPos;
    }

    public double getRightPos() {
        return rightPos;
    }

    public void overrideMaxVel(boolean overrideMaxVel) {
        this.overrideMaxVel = overrideMaxVel;
    }

    public void toggleOverride() {
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
