package bezier;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class Bezier {

    public static ArrayList<Point> generate(ArrayList<Point> controlPoints, double maxVel, double maxAccel, double time) {
        ArrayList<Point> pathPoints = new ArrayList<>();

        ArrayList<Point> throughPoints = (ArrayList<Point>) controlPoints.stream()
                .filter(Point::isIntercept)
                .collect(Collectors.toList());

        time /= throughPoints.size()-1;
        double startMaxVel = maxVel;
        for (int j = 0; j < throughPoints.size() - 1; j++) {
            double startVel = throughPoints.get(j).getTargetVelocity(),
                    endVel = throughPoints.get(j+1).getTargetVelocity();
            if (controlPoints.get(j+1).isOverrideMaxVel()) maxVel = Math.max(startVel, endVel);
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
                double up = Math.min(maxAccel * t * time + startVel, maxVel),              //TODO make going backwards work
                        down = Math.min(-maxAccel * (t * time - time) + endVel, maxVel);    //TODO make work with dragging
                pathPoints.get(pathPoints.size() - 1).setTargetVelocity(Math.min(up, down));
            }
            if (pathPoints.get(pathPoints.size() - 1).getTargetVelocity() != endVel) {
                throughPoints.get(j+1).setTargetVelocity(pathPoints.get(pathPoints.size() - 1).getTargetVelocity());
            }
        }
        if (pathPoints.isEmpty()) return pathPoints;
        pathPoints.get(pathPoints.size() - 1).setLast(true);
        for (int i = 0; i < pathPoints.size(); i++) {
            Point p = pathPoints.get(i);
            if (p.isLast()) {
                p.setAngle(pathPoints.get(i - 1).getAngle());
            } else {
                p.setAngle(p.angleTo(pathPoints.get(i + 1)));
            }
        }
        return pathPoints;
    }

    public static ArrayList<Point> motion(ArrayList<Point> path, double axleLength) { //TODO double tuning factor (to go from actual velocity to the num the Talon wants)
        ArrayList<Point[]> circles = assignCircles(path);
        ArrayList<Double> radii = assignRadii(circles);
        ArrayList<Point> motivePath = new ArrayList<>();
        for (Point[] circle : circles) {
            Point p = new Point(circle[0].getX(), circle[0].getY());
            p.setAngle(circle[0].getAngle());
            double radius = radii.get(circles.indexOf(circle));
            double rightVel = (p.getTargetVelocity() * (radius + axleLength / 2)) / radius,
                    leftVel = (p.getTargetVelocity() * (radius - axleLength / 2)) / radius;
            p.setVels(leftVel, rightVel);
        }
        return motivePath;
    }

    private static ArrayList<Point[]> assignCircles(ArrayList<Point> path) {
        ArrayList<Point[]> circles = new ArrayList<>();
        for (int i = 0; i < path.size() - 2; i++) { //exclude last two points
            Point[] circlePoints = new Point[3];
            for (int j = 0; j < 3; j++)
                circlePoints[j] = path.get(i + j);
            circles.add(circlePoints);
        }
        return circles;
    }

    private static ArrayList<Double> assignRadii(ArrayList<Point[]> circles) {
        ArrayList<Double> radii = new ArrayList<>();
        for (Point[] circle : circles) {
            double a = circle[0].distanceTo(circle[1]),
                    b = circle[1].distanceTo(circle[2]),
                    c = circle[2].distanceTo(circle[0]);
            double s = (a + b + c) / 2;
            double area = Math.sqrt(s * (s - a) * (s - b) * (s - c));
            double radius = (a * b * c) / (4 * area);
            radii.add(radius);
        }
        return radii;
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
