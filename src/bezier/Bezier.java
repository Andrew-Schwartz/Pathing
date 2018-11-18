package bezier;

import ui.UIController;
import utils.UnitConverter;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class Bezier {

    public static ArrayList<Point> generate(ArrayList<Point> controlPoints) {
        double maxVel = UIController.config.getDoubleProperty("max_vel"),
                maxAccel = UIController.config.getDoubleProperty("max_accel"),
                time = UIController.config.getDoubleProperty("time");

        ArrayList<Point> pathPoints = new ArrayList<>();
        ArrayList<Point> throughPoints = (ArrayList<Point>) controlPoints.stream()
                .filter(Point::isIntercept)
                .collect(Collectors.toList());

        time /= throughPoints.size() - 1;
        double startMaxVel = maxVel;
        for (int j = 0; j < throughPoints.size() - 1; j++) {
            double startVel = throughPoints.get(j).getTargetVelocity(),
                    endVel = throughPoints.get(j + 1).getTargetVelocity();
            if (controlPoints.get(j + 1).isOverrideMaxVel()) maxVel = Math.max(startVel, endVel);
            else maxVel = startMaxVel;
            for (double t = 0.0; t <= 1; t += 1. / 300.) {
                double sumX = 0, sumY = 0;
                double T = 1 - t;
                int startPoint = controlPoints.indexOf(throughPoints.get(j));
                int endPoint = controlPoints.indexOf(throughPoints.get(j + 1));
                for (int i = 0; i <= endPoint - startPoint; i++) {
                    int I = (endPoint - startPoint) - i;
                    sumX += polynomialCoeff(endPoint - startPoint, i) * Math.pow(T, i) * Math.pow(t, I) * controlPoints.get(I + startPoint).getX();
                    sumY += polynomialCoeff(endPoint - startPoint, i) * Math.pow(T, i) * Math.pow(t, I) * controlPoints.get(I + startPoint).getY();
                }
                pathPoints.add(new Point(sumX, sumY));
                //trapezoidal velocities
                double up = Math.min(maxAccel * t * time + startVel, maxVel),
                        down = Math.min(-maxAccel * (t * time - time) + endVel, maxVel);
                pathPoints.get(pathPoints.size() - 1).setTargetVelocity(Math.min(up, down));
            }
            if (pathPoints.get(pathPoints.size() - 1).getTargetVelocity() != endVel) {
                throughPoints.get(j + 1).setTargetVelocity(pathPoints.get(pathPoints.size() - 1).getTargetVelocity());
            }
        }
        if (pathPoints.isEmpty()) return pathPoints;
        pathPoints.get(pathPoints.size() - 1).setLast(true);
        for (int i = 0; i < pathPoints.size(); i++) {
            Point p = pathPoints.get(i);
            if (p.isLast()) {
                p.setHeading(pathPoints.get(i - 1).getHeading());
            } else {
                p.setHeading(p.angleTo(pathPoints.get(i + 1)));
            }
        }
        return pathPoints;
    }

    /**
     * <p>Calculates velocity and position of each point in the path</p>
     * <p>turning left, equation is:</p>
     * <p>Vr = w * (R + l/2)</p>
     * <p>Vl = w * (R - l/2)</p>
     *
     * @param path contains x,y,theta coordinates of each point, and the target velocity to travel at (if going in a line)
     */
    public static void motion(ArrayList<Point> path) {
        double axleLength = UIController.config.getDoubleProperty("width"),
                wheelRadius = UIController.config.getDoubleProperty("wheel_radius");
        double halfWidth = axleLength / 2;
        for (int i = 0; i < path.size(); i++) {
            double circleRadius;
            if (i + 2 > path.size() - 1) {
                circleRadius = radiusOfCircle(path.get(path.size() - 1), path.get(path.size() - 2), path.get(path.size() - 3));
            } else {
                circleRadius = UnitConverter.pixelsToInches(radiusOfCircle(path.get(i), path.get(i + 1), path.get(i + 2)));
            }
            double leftVel, rightVel;
            if (Double.isInfinite(circleRadius)) { //linear path
                leftVel = rightVel = UnitConverter.feetToInchs(path.get(i).getTargetVelocity());
                System.out.printf("Point %-3d is infinite\n", i);
            } else {
                double angularVel = UnitConverter.feetToInchs(path.get(i).getTargetVelocity()) / circleRadius;
                if (path.get(i + 2).getHeadingCartesian() < path.get(i).getHeadingCartesian()) { // turning left
                    leftVel = angularVel * (circleRadius - halfWidth);
                    rightVel = angularVel * (circleRadius + halfWidth);
                } else { //turning right
                    leftVel = angularVel * (circleRadius + halfWidth);
                    rightVel = angularVel * (circleRadius - halfWidth);
                }
            }
            leftVel = leftVel / (wheelRadius * Math.PI * 2);
            rightVel = rightVel / (wheelRadius * Math.PI * 2);
            path.get(i).setVels(leftVel, rightVel);
            if (i == 0) path.get(i).setPos(leftVel, rightVel);
            else path.get(i).advancePos(path.get(i - 1).getLeftPos(), path.get(i - 1).getRightPos());
        }
    }

    /**
     * <p>calculates the radius of the circle which the 3 points lie on</p>
     * <p>r = a*b*c/4*area</p>
     *
     * @param circlePoints 3 points from which a circle is extrapolated
     * @return radius of the circle in pixels
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

    private static int polynomialCoeff(int line, int n) {
        int result = 1;
        for (int i = 0; i < n; i++) {
            result *= line - i;
            result /= i + 1;
        }
        return result;
    }
}
