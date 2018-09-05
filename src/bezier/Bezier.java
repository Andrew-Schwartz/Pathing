package bezier;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class Bezier {

    public static ArrayList<Point> generate(ArrayList<Point> controlPoints) {
        ArrayList<Point> pathPoints = new ArrayList<>();

//        ArrayList<Point> throughPoints = (ArrayList<Point>) controlPoints.stream()
//                .filter(Point::isIntercept)
//                .collect(Collectors.toList());

        ArrayList<Point> throughPoints = new ArrayList<>();
        for (Point p : controlPoints) {
            if (p.isIntercept())
                throughPoints.add(p);
        }

        for (int j = 0; j < throughPoints.size() - 1; j++) {
            for (double t = 0.0; t <= 1; t += 1./300.) {
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
            }
        }
        return pathPoints;
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
