package bezier;

import utils.Config;
import utils.UnitConverter;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class Bezier {

    public static ArrayList<Point> generateAll(ArrayList<Point> controlPoints) {
        ArrayList<Point> pathPureXY = generatePureXY(controlPoints);
        ArrayList<Double> times = trapezoidalTimes(pathPureXY);
        ArrayList<Point> path = generateWithVel(controlPoints, times);
        motion(path);
        return path;
    }

    public static ArrayList<Point> generatePureXY(ArrayList<Point> controlPoints) {
        ArrayList<Point> path = new ArrayList<>();
        ArrayList<Point> throughPoints = (ArrayList<Point>) controlPoints.stream()
                .filter(Point::isIntercept)
                .collect(Collectors.toList());
        for (int j = 0; j < throughPoints.size() - 1; j++) {
            for (double t = 0.0; t <= 1; t += 1. / 299.) { //some large number of points
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
//                p.setDistanceTo(path.get(i - 1));
            }
        }
        path.get(path.size() - 1).setHeading(path.get(path.size() - 2).getHeading());
        return path;
    }

    public static ArrayList<Double> trapezoidalTimes(ArrayList<Point> pathXY) { //TODO work with end vels != 0
        if (pathXY.isEmpty()) return new ArrayList<>();
        double velMax = Config.getDoubleProperty("max_vel"),
                accelMax = Config.getDoubleProperty("max_accel");
        ArrayList<Double> times = new ArrayList<>();
        ArrayList<Point> throughPoints = (ArrayList<Point>) pathXY.stream()
                .filter(Point::isIntercept)
                .collect(Collectors.toList());
        for (int j = 0; j < throughPoints.size() - 1; j++) {
            double lengthOfCurve = throughPoints.get(j + 1).getDistance() - throughPoints.get(j).getDistance();
            lengthOfCurve = UnitConverter.pixelsToFeet(lengthOfCurve);
            double velInitial = 0; //TODO not just 0 -- pointrow.get(j).getPoint().isOverrideMaxVel()
            //if triangle, quadratic; if trapezoid vel. the smaller is the always the right one //TODO "ALWAYS"?
            double timeAccelTrapezoid = (velMax - velInitial) / accelMax; //t = deltaV / a
            double timeAccelTriangle = quadratic(0.5 * accelMax, velInitial, -lengthOfCurve / 2); //x = v0t + 1/2at^2
            if (timeAccelTrapezoid < timeAccelTriangle) { //is a trapezoid
                double timeConst = (lengthOfCurve - 2 * timeAccelTrapezoid * (velInitial + velMax)) / velMax;
                times.add(timeAccelTrapezoid + timeConst + timeAccelTrapezoid);
            } else { //is a triangle
                times.add(timeAccelTriangle + timeAccelTriangle);
            }
        }
        return times;
    }


    public static ArrayList<Point> generateWithVel(ArrayList<Point> controlPoints, ArrayList<Double> times) {
        double maxVel = Config.getDoubleProperty("max_vel"),
                maxAccel = Config.getDoubleProperty("max_accel"),
                timeStep = Config.getDoubleProperty("time_step");

        ArrayList<Point> path = new ArrayList<>();
        ArrayList<Point> throughPoints = (ArrayList<Point>) controlPoints.stream()
                .filter(Point::isIntercept)
                .collect(Collectors.toList());

        double startMaxVel = maxVel;
        for (int j = 0; j < throughPoints.size() - 1; j++) {
            double startVel = throughPoints.get(j).getTargetVelocity(),
                    endVel = throughPoints.get(j + 1).getTargetVelocity(); //problem is that is is being changed in prior runnings of this method
            if (controlPoints.get(j + 1).isOverrideMaxVel()) maxVel = Math.max(startVel, endVel);
            else maxVel = startMaxVel;
            double time = times.get(j);
//            for (double t = 0.0; t <= 1; t += 1. / (time / timeStep)) {
            double precision = Math.ceil(time / timeStep);
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
                double up = Math.min(maxAccel * t * time + startVel, maxVel),
                        down = Math.min(-maxAccel * (t * time - time) + endVel, maxVel);
                path.get(path.size() - 1).setTargetVelocity(Math.min(up, down));
                path.get(path.size() - 1).setTime(time * t);
            }
            if (path.get(path.size() - 1).getTargetVelocity() != endVel) {
                throughPoints.get(j + 1).setTargetVelocity(path.get(path.size() - 1).getTargetVelocity());
            }
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
        double axleLength = Config.getDoubleProperty("width");
        double halfWidth = axleLength / 2;
        for (int i = 0; i < path.size(); i++) {
            int iAdjusted;
            if (i + 2 > path.size() - 1)
                iAdjusted = path.size() - 3;
            else
                iAdjusted = i;
            double circleRadius = UnitConverter.pixelsToInches(radiusOfCircle(path.get(iAdjusted), path.get(iAdjusted + 1), path.get(iAdjusted + 2)));
            double leftVel, rightVel;
            if (Double.isInfinite(circleRadius)) { //linear path
                leftVel = UnitConverter.feetToInches(path.get(i).getTargetVelocity());
                rightVel = UnitConverter.feetToInches(path.get(i).getTargetVelocity());
                //                System.out.printf("Point %-3d is infinite\n", i);
            } else {
                double angularVel = UnitConverter.feetToInches(path.get(i).getTargetVelocity()) / circleRadius;
                if (path.get(iAdjusted + 2).getHeadingCartesian() < path.get(iAdjusted).getHeadingCartesian()) { // turning left
                    leftVel = angularVel * (circleRadius - halfWidth);
                    rightVel = angularVel * (circleRadius + halfWidth);
                } else { //turning right
                    leftVel = angularVel * (circleRadius + halfWidth);
                    rightVel = angularVel * (circleRadius - halfWidth);
                }
            }
            leftVel = UnitConverter.linearToRotational(leftVel);
            rightVel = UnitConverter.linearToRotational(rightVel);
            path.get(i).setVels(leftVel, rightVel);
            if (i == 0)
                path.get(i).setPos(UnitConverter.rotationalToLinear(leftVel), UnitConverter.rotationalToLinear(rightVel));
            else path.get(i).advancePos(path.get(i - 1).getLeftPos(), path.get(i - 1).getRightPos());
        }
    }

//    public static ArrayList<Point> secondPass(ArrayList<Point> motivePath) {
//        double pathDist = 0;
//        for (int i = 0; i < motivePath.size() - 1; i++) {
//            pathDist += motivePath.get(i).distanceTo(motivePath.get(i + 1));
//        }
//        pathDist = UnitConverter.pixelsToInches(pathDist);
//        System.out.println(pathDist);
//        int finalPoint = -1;
//        for (int i = 0; i < motivePath.size(); i++) {
//            double leftDist = motivePath.get(i).getLeftPos(),
//                    rightDist = motivePath.get(i).getRightPos();
//            double avg = (leftDist + rightDist) / 2;
//            if (avg > pathDist) {
//                System.out.printf("i: %d,\tavg: %f\n", i, avg);
//                finalPoint = i;
//                break;
//            }
//        }
//        return motivePath;
//    }

    /**
     * <p>calculates the radius of the circle which the 3 points lie on</p>
     * <p>r = a*b*c/4*area</p>
     *
     * @param circlePoints 3 points from which a circle is extrapolated
     * @return radius of the circle in same unit as represented in the circlePoints (pixels)
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
