package bezier;

import javafx.scene.control.Alert;

import java.util.ArrayList;

import static java.util.stream.Collectors.toList;
import static utils.Config.*;
import static utils.UnitConverter.*;

public class Bezier {

    public static ArrayList<Point> generateAll(ArrayList<Point> controlPoints) {
        ArrayList<Point> pathPureXY = generatePureXY(controlPoints);
        ArrayList<Double> times = trapezoidalTimes(pathPureXY, controlPoints);
        ArrayList<Point> path = generateWithVel(controlPoints, times);
        motion(path);
        return path;
    }

    public static ArrayList<Point> generatePureXY(ArrayList<Point> controlPoints) {
        ArrayList<Point> path = new ArrayList<>();
        ArrayList<Point> throughPoints = (ArrayList<Point>) controlPoints.stream()
                .filter(Point::isIntercept)
                .collect(toList());
        for (int j = 0; j < throughPoints.size() - 1; j++) {
            for (double t = 0.0; t <= 1; t += 1. / 299.) { //some large number of saves.points
                double sumX = 0, sumY = 0;
                double T = 1 - t;
                int startPoint = controlPoints.indexOf(throughPoints.get(j));
                int endPoint = controlPoints.indexOf(throughPoints.get(j + 1));
                for (int i = 0; i <= endPoint - startPoint; i++) {
                    int I = (endPoint - startPoint) - i;
                    sumX += polynomialCoeff(endPoint - startPoint, i) * Math.pow(T, i) * Math.pow(t, I) * controlPoints.get(I + startPoint).getX();
                    sumY += polynomialCoeff(endPoint - startPoint, i) * Math.pow(T, i) * Math.pow(t, I) * controlPoints.get(I + startPoint).getY();
                }
                path.add(new Point(sumX, sumY));
                if (t == 0)
                    path.get(path.size() - 1).setIntercept(true); //start and end of each segment is at same point as an intercept
            }
        }
        if (path.isEmpty()) return new ArrayList<>();
        path.get(0).setDistance(0);
        path.get(path.size() - 1).setIntercept(true);
        for (int i = 0; i < path.size(); i++) {
            Point p = path.get(i);
            if (i != path.size() - 1) {
                p.setHeadingTo(path.get(i + 1));
            }
            if (i != 0) {
                p.setDistance(path.get(i - 1).getDistance() + p.distanceTo(path.get(i - 1)));
            }
        }
        path.get(path.size() - 1).setHeading(path.get(path.size() - 2).getHeading());
        return path;
    }

    public static ArrayList<Double> trapezoidalTimes(ArrayList<Point> pathXY, ArrayList<Point> controlPoints) {
        if (pathXY.isEmpty()) return new ArrayList<>();
        ArrayList<Double> times = new ArrayList<>();
        ArrayList<Point> throughPoints = (ArrayList<Point>) pathXY.stream()
                .filter(Point::isIntercept)
                .collect(toList());
        ArrayList<Point> oldThroughPoints = (ArrayList<Point>) controlPoints.stream()
                .filter(Point::isIntercept)
                .collect(toList());
        for (int j = 0; j < throughPoints.size() - 1; j++) {
            double lengthOfCurve = throughPoints.get(j + 1).getDistance() - throughPoints.get(j).getDistance();
            lengthOfCurve = inchesToFeet(lengthOfCurve);
            double velInitial = oldThroughPoints.get(j).getTargetVelocity();
            double velFinal = oldThroughPoints.get(j + 1).getTargetVelocity();
            double velMax = oldThroughPoints.get(j + 1).isOverrideMaxVel()
                    ? oldThroughPoints.get(j + 1).getTargetVelocity()
                    : maxVel();

            if (velMax == 0) {
                String errorMsg = "with a max speed of 0, you'll never get anywhere!";
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("Impossible Physics");
                alert.setContentText(errorMsg);
                alert.showAndWait();
                throw new IllegalStateException(errorMsg);
            }

            //if accel and deccel take same time, calculations are much easier
            double velInitialAndFinal = velInitial;
            double timeAccelInitToFinal = 0;
            if (velInitial != velFinal) {
                timeAccelInitToFinal = Math.abs(velInitial - velFinal) / maxAccel();
                double distToEqualizeVels = timeAccelInitToFinal * (velInitial + velFinal) / 2.;
                velInitialAndFinal = Math.min(velInitial, velFinal) + Math.abs(velInitial - velFinal);

                if (distToEqualizeVels > lengthOfCurve) {
                    String errorMsg = "distance traveled while accelerating from initial to final velocity ("
                            + distToEqualizeVels + ") is greater than total distance of path (" + lengthOfCurve + ")";
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setHeaderText("Impossible Physics");
                    alert.setContentText(errorMsg);
                    alert.showAndWait();
                    throw new IllegalStateException(errorMsg);
                }
                lengthOfCurve -= distToEqualizeVels;
            }

            //calculate max vel that is reachable physically
            double timeAccelTriangle = quadratic(0.5 * maxAccel(), velInitial, -lengthOfCurve / 2); //x = v0t + 1/2at^2
            double velMaxReachable = Math.min(velInitialAndFinal + maxAccel() * timeAccelTriangle, velMax);

            double timeAccel = (velMaxReachable - velInitialAndFinal) / maxAccel(),
                    timeDeccel = (velInitialAndFinal - velMaxReachable) / -maxAccel();
            double timeConst = (lengthOfCurve - ((timeAccel * (velInitialAndFinal + velMaxReachable) / 2) + (timeDeccel * (velInitialAndFinal + velMaxReachable) / 2))) / velMaxReachable;
            times.add(timeAccelInitToFinal + timeAccel + timeConst + timeDeccel);

//            //if triangle, quadratic; if trapezoid vel. the smaller is the always the right one //TODO "ALWAYS"?
//            double timeAccelTrapezoid = (velMax - velInitial) / maxAccel(); //t = deltaV / a
//            if (timeAccelTrapezoid < timeAccelTriangle) { //is a trapezoid
//                double timeConst = (lengthOfCurve - 2 * timeAccelTrapezoid * (velInitial + velMax)) / maxAccel();
//                times.add(timeAccelTrapezoid + timeConst + timeAccelTrapezoid);
//            } else { //is a triangle
//                times.add(timeAccelTriangle + timeAccelTriangle);
//            }
        }
        return times;
    }

    public static ArrayList<Point> generateWithVel(ArrayList<Point> controlPoints, ArrayList<Double> times) {
        ArrayList<Point> path = new ArrayList<>();
        ArrayList<Point> throughPoints = (ArrayList<Point>) controlPoints.stream()
                .filter(Point::isIntercept)
                .collect(toList());

        for (int j = 0; j < throughPoints.size() - 1; j++) {
            double startVel = throughPoints.get(j).getTargetVelocity(),
                    endVel = throughPoints.get(j + 1).getTargetVelocity(); //problem is that is is being changed in prior runnings of this method
            double curMaxVel = maxVel();
            if (controlPoints.get(j + 1).isOverrideMaxVel())
                curMaxVel = Math.max(startVel, endVel);
            double time = times.get(j);
            double precision = Math.ceil(time / timeStep()) + 1;
            for (int fakeT = 0; fakeT <= precision; fakeT++) {
                double t = fakeT / precision;
                double sumX = 0, sumY = 0;
                double T = 1 - t;
                int startPoint = controlPoints.indexOf(throughPoints.get(j));
                int endPoint = controlPoints.indexOf(throughPoints.get(j + 1));
                for (int i = 0; i <= endPoint - startPoint; i++) {
                    int I = (endPoint - startPoint) - i;
                    sumX += polynomialCoeff(endPoint - startPoint, i) * Math.pow(T, i) * Math.pow(t, I) * controlPoints.get(I + startPoint).getX();
                    sumY += polynomialCoeff(endPoint - startPoint, i) * Math.pow(T, i) * Math.pow(t, I) * controlPoints.get(I + startPoint).getY();
                }
                path.add(new Point(sumX, sumY));
                //trapezoidal velocities
                double up = Math.min(maxAccel() * t * time + startVel, curMaxVel),
                        down = Math.min(-maxAccel() * (t * time - time) + endVel, curMaxVel);
                path.get(path.size() - 1).setTargetVelocity(Math.min(up, down));
                path.get(path.size() - 1).setTime(time * t);
            }
            if (throughPoints.get(j + 1).isReverse())
                path.stream().skip((long) (path.size() - precision)).forEach(Point::reverse);
            if (j != throughPoints.size() - 2 && !path.isEmpty()) path.remove(path.size() - 1);
        }
        if (path.isEmpty()) return new ArrayList<>();
        path.get(0).setDistance(0);
        for (int i = 0; i < path.size(); i++) {
            Point p = path.get(i);
            if (i != path.size() - 1) {
                p.setHeadingTo(path.get(i + 1));
            }
            if (i != 0) {
                p.setDistance(path.get(i - 1).getDistance() + p.distanceTo(path.get(i - 1)));
            }
        }
        path.get(path.size() - 1).setHeading(path.get(path.size() - 2).getHeading());
        return path;
    }

    /**
     * <p>Calculates velocity and position of each point in the path</p>
     * <p>turning left, equation is:</p>
     * <p>Vr = w * (R + l/2)</p>
     * <p>Vl = w * (R - l/2)</p>
     *
     * @param path contains x,y,theta coordinates of each point, and the target velocity to travel at (as if going in a line)
     */
    public static void motion(ArrayList<Point> path) {
        if (path.size() == 0) return;
        double halfWidth = width() / 2;
        for (int i = 0; i < path.size(); i++) {
            int iAdjusted = i;
            if (i + 2 > path.size() - 1)
                iAdjusted = path.size() - 3;
            double circleRadius = radiusOfCircle(path.get(iAdjusted), path.get(iAdjusted + 1), path.get(iAdjusted + 2));
            double leftVel, rightVel;
            if (Double.isInfinite(circleRadius)) { //linear path
                leftVel = feetToInches(path.get(i).getTargetVelocity());
                rightVel = feetToInches(path.get(i).getTargetVelocity());
            } else {
                double angularVel = feetToInches(path.get(i).getTargetVelocity()) / circleRadius;
                if (path.get(iAdjusted + 2).getHeadingCartesian() < path.get(iAdjusted).getHeadingCartesian()) { // turning left
                    leftVel = angularVel * (circleRadius - halfWidth);
                    rightVel = angularVel * (circleRadius + halfWidth);
                } else { //turning right
                    leftVel = angularVel * (circleRadius + halfWidth);
                    rightVel = angularVel * (circleRadius - halfWidth);
                }
            }
            leftVel = linearToRotational(leftVel);
            rightVel = linearToRotational(rightVel);
            path.get(i).setVels(leftVel, rightVel);
            if (i == 0) {
                path.get(i).setPos(rotationalToLinear(0), rotationalToLinear(0));
            } else path.get(i).advancePos(path.get(i - 1).getLeftPos(), path.get(i - 1).getRightPos());
        }
    }

    /**
     * <p>calculates the radius of the circle which the 3 saves.points lie on</p>
     * <p>r = a*b*c/4*area</p>
     *
     * @param circlePoints 3 saves.points from which a circle is extrapolated
     * @return radius of the circle in same unit as represented in the circlePoints in inches
     */
    private static double radiusOfCircle(Point... circlePoints) {
        assert circlePoints.length == 3;
        double a = circlePoints[0].distanceTo(circlePoints[1]),
                b = circlePoints[1].distanceTo(circlePoints[2]),
                c = circlePoints[2].distanceTo(circlePoints[0]);
        double s = (a + b + c) / 2;
        a = Math.min(s, a);     //fix rounding error
        b = Math.min(s, b);     //if any were > s, NaN results
        c = Math.min(s, c);
        double area = Math.sqrt(s * (s - a) * (s - b) * (s - c));
        return (a * b * c) / (4 * area);
    }

    private static double quadratic(double a, double b, double c) {
        return Math.max(
                (-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a),
                (-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a));
    }

    private static int polynomialCoeff(int line, int n) {
        int result = 1;
        for (int i = 0; i < n; i++) {
            result *= line - i;
            result /= i + 1;
        }
        return result;
    }
}
