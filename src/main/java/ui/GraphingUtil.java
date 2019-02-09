package ui;

import bezier.GraphicalBezier;
import bezier.Point;
import bezier.units.Degrees;
import bezier.units.Feet;
import bezier.units.Inches;
import bezier.units.Rotation2d;
import bezier.units.Seconds;
import bezier.units.derived.LinearVelocity;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tab;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import utils.Config;

import java.util.ArrayList;
import java.util.function.Function;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toCollection;
import static ui.UIController.imageHeight;
import static utils.Config.maxAccel;
import static utils.Config.timeStep;
import static utils.Config.width;

public class GraphingUtil {
    private ArrayList<Point> controlPoints, path;
    private ArrayList<PointRow> rows;
    private final Polyline polyPos;
    private final Polyline polyLeft;
    private final Polyline polyRight;
    private final LineChart<Double, Double> chtLeft;
    private final LineChart<Double, Double> chtRight;
    private final LineChart<Double, Double> chtCenter;
    private final Tab tabVel;
    private AnchorPane imgPane;
    private final ArrayList<Circle> circles;

    public GraphingUtil(ArrayList<Point> controlPoints, ArrayList<Point> path, ArrayList<PointRow> rows,
                        Polyline polyPos, Polyline polyLeft, Polyline polyRight,
                        LineChart<Double, Double> chtLeft, LineChart<Double, Double> chtRight, LineChart<Double, Double> chtCenter,
                        Tab tabVel, AnchorPane imgPane) {
        this.controlPoints = controlPoints;
        this.path = path;
        this.rows = rows;
        this.polyPos = polyPos;
        this.polyLeft = polyLeft;
        this.polyRight = polyRight;
        this.chtLeft = chtLeft;
        this.chtRight = chtRight;
        this.chtCenter = chtCenter;
        this.tabVel = tabVel;
        this.imgPane = imgPane;
        circles = new ArrayList<>();
    }

    public void updatePolyline(boolean highlightPoints) {
        if (highlightPoints) highlightPoints();

        controlPoints = rows.stream()
                .sorted(comparingInt(PointRow::getIndex))
                .map(PointRow::getPoint)
                .collect(toCollection(ArrayList::new));

        path = GraphicalBezier.generateSpline(controlPoints);

        polyPos.getPoints().clear();
        path.forEach(point -> polyPos.getPoints().addAll(point.getX().pixels().getValue(),
                imageHeight().minus(point.getY().pixels()).getValue()));

        graphMotion();

        polyLeft.getPoints().clear();
        polyRight.getPoints().clear();

        if (path.isEmpty()) {
            polyPos.getPoints().addAll(0.0, 0.0);  //polyline with no points doesn't redraw
            polyLeft.getPoints().addAll(0.0, 0.0); //so this does
            polyRight.getPoints().addAll(0.0, 0.0);
            return;
        }

        switch (WheelPathType.valueOf(Config.getStringProperty("draw_wheels_type"))) {
            case PATH:
                pathFromPath();
                break;
            case VEL:
                pathFromVel();
                break;
            case NONE:
            default:
                polyLeft.getPoints().addAll(0.0, 0.0);
                polyRight.getPoints().addAll(0.0, 0.0);
        }
    }

    public void highlightPoints() {
        clearCircles();
        rows.stream()
                .sorted(comparingInt(PointRow::getIndex))
                .map(PointRow::getPoint)
                .forEach(this::addCircle);
        imgPane.getChildren().addAll(circles);
    }

    private void addCircle(Point point) {
        circles.add(new Circle(
                point.getX().pixels().getValue(),
                imageHeight().minus(point.getY().pixels()).getValue(),
                3,
                Color.ORANGE)
        );
    }

    public void clearCircles() {
        imgPane.getChildren().removeIf(circles::contains);
        circles.clear();
    }

    private void pathFromPath() {
//        final Inches dist = width().div(2);
        for (Point point : path) {
            point.setLeftAndRightPositions();
            polyLeft.getPoints().addAll(point.getLeftPoint().getX().pixels().getValue(),
                    (UIController.imageHeight().minus(point.getLeftPoint().getY().pixels())).getValue());

            polyRight.getPoints().addAll(point.getRightPoint().getX().pixels().getValue(),
                    (UIController.imageHeight().minus(point.getRightPoint().getY().pixels()).getValue()));

//            Degrees angle = point.getHeading();
//            Pixels offsetX = dist.times(angle.radians().getCos()).pixels();
//            Pixels offsetY = dist.times(angle.radians().getSin()).pixels();

//            polyLeft.getPoints().addAll((point.getX().pixels().minus(offsetX)).getValue(),
//                    imageHeight().minus(point.getY().pixels().plus(offsetY)).getValue());

//            polyRight.getPoints().addAll((point.getX().pixels().plus(offsetX)).getValue(),
//                    imageHeight().minus(point.getY().pixels().minus(offsetY)).getValue());
        }
    }

    private void pathFromVel() {
        final Inches dist = width().div(2);

        Degrees angle = path.get(0).getHeading();
        Inches offsetX = dist.times(angle.radians().getCos());
        Inches offsetY = dist.times(angle.radians().getSin());

        Inches xl = path.get(0).getX().minus(offsetX);
        Inches yl = imageHeight().inches().minus(path.get(0).getY()).plus(offsetY);
        Inches xr = path.get(0).getX().plus(offsetX);
        Inches yr = imageHeight().inches().minus(path.get(0).getY().minus(offsetY));

        polyLeft.getPoints().addAll(xl.pixels().getValue(), yl.pixels().getValue());
        polyRight.getPoints().addAll(xr.pixels().getValue(), yr.pixels().getValue());

        for (Point point : path) {
            Degrees between = (new Point(xl, yl).angleTo(new Point(xr, yr)));
            xl = xl.plus(pointDistFromVel(point.getLeftVel(), between, Rotation2d::getCos));
            yl = yl.minus(pointDistFromVel(point.getRightVel(), between, Rotation2d::getSin));
            polyLeft.getPoints().addAll(xl.pixels().getValue(), yl.pixels().getValue());

            xr = xr.plus(pointDistFromVel(point.getLeftVel(), between, Rotation2d::getCos));
            yr = yr.minus(pointDistFromVel(point.getRightVel(), between, Rotation2d::getSin));
            polyRight.getPoints().addAll(xr.pixels().getValue(), yr.pixels().getValue());
        }
    }

    private Inches pointDistFromVel(LinearVelocity<Inches, Seconds> vel, Degrees between, Function<Rotation2d, Double> cosOrSin) {
        Inches distTraveled = vel.times(timeStep());
        double scaleFactor = cosOrSin.apply(between.radians());
        return distTraveled.times(scaleFactor);
    }


    public void graphMotion() {
        if (!tabVel.isSelected()) return;
        chtLeft.getData().clear();
        chtRight.getData().clear();
        chtCenter.getData().clear();
        XYChart.Series<Double, Double> leftPos = new XYChart.Series<>(),
                leftVel = new XYChart.Series<>(),
                leftAccel = new XYChart.Series<>(),
                rightPos = new XYChart.Series<>(),
                rightVel = new XYChart.Series<>(),
                rightAccel = new XYChart.Series<>(),
                centerPos = new XYChart.Series<>(),
                centerVel = new XYChart.Series<>(),
                centerAccel = new XYChart.Series<>();
        leftPos.setName("pos");
        leftVel.setName("vel");
        leftAccel.setName("accel");
        rightPos.setName("pos");
        rightVel.setName("vel");
        rightAccel.setName("accel");
        centerPos.setName("pos");
        centerVel.setName("vel");
        centerAccel.setName("accel");
        Inches totalDist = new Inches(0.0);
        for (int i = 0; i < path.size(); i++) {
            Seconds curTime = path.get(path.size() - 1).getTime().times(i).div(path.size());
            var deltaLeftVel = i == 0
                    ? new LinearVelocity<>(new Feet(0.0), new Seconds(1.0))
                    : path.get(i).getLeftVel().minus(path.get(i - 1).getLeftVel()).feetPerSecond();
            var deltaRightVel = i == 0
                    ? new LinearVelocity<>(new Feet(0.0), new Seconds(1.0))
                    : path.get(i).getRightVel().minus(path.get(i - 1).getRightVel()).feetPerSecond();
            var deltaCenterVel = i == 0
                    ? new LinearVelocity<>(new Feet(0.0), new Seconds(1.0))
                    : path.get(i).getTargetVelocity().minus(path.get(i - 1).getTargetVelocity()).feetPerSecond();
            totalDist = totalDist.plus(path.get(i).getTargetVelocity().times(timeStep()));

            Function<LinearVelocity<Feet, Seconds>, Double> boundedAccel = vel -> {
                double maxAccel = maxAccel().getValue();
                double accelValue = vel.div(timeStep()).getValue();
                return Math.max(-maxAccel, Math.min(maxAccel, accelValue));
            };

            leftPos.getData().add(new XYChart.Data<>(curTime.getValue(), path.get(i).getLeftPos().feet().getValue()));
            leftVel.getData().add(new XYChart.Data<>(curTime.getValue(), path.get(i).getLeftVel().feetPerSecond().getValue()));
            leftAccel.getData().add(new XYChart.Data<>(curTime.getValue(), boundedAccel.apply(deltaLeftVel)));

            rightPos.getData().add(new XYChart.Data<>(curTime.getValue(), path.get(i).getRightPos().feet().getValue()));
            rightVel.getData().add(new XYChart.Data<>(curTime.getValue(), path.get(i).getRightVel().feetPerSecond().getValue()));
            rightAccel.getData().add(new XYChart.Data<>(curTime.getValue(), boundedAccel.apply(deltaRightVel)));

            centerPos.getData().add(new XYChart.Data<>(curTime.getValue(), totalDist.feet().getValue()));
            centerVel.getData().add(new XYChart.Data<>(curTime.getValue(), path.get(i).getTargetVelocity().feetPerSecond().getValue()));
            centerAccel.getData().add(new XYChart.Data<>(curTime.getValue(), boundedAccel.apply(deltaCenterVel)));
        }
        chtLeft.getData().addAll(leftPos, leftVel, leftAccel);
        chtRight.getData().addAll(rightPos, rightVel, rightAccel);
        chtCenter.getData().addAll(centerPos, centerVel, centerAccel);
    }


    public ArrayList<Point> getControlPoints() {
        return controlPoints;
    }

    public ArrayList<Point> getPath() {
        return path;
    }

    public ArrayList<PointRow> getRows() {
        return rows;
    }
}
