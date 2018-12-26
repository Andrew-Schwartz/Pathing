package ui;

import bezier.Bezier;
import bezier.Point;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tab;
import javafx.scene.shape.Polyline;
import utils.Config;
import utils.Utils;

import java.util.ArrayList;
import java.util.function.Function;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static ui.UIController.imageHeight;
import static utils.Config.maxAccel;
import static utils.Config.timeStep;
import static utils.Config.width;
import static utils.UnitConverter.feetToPixels;
import static utils.UnitConverter.inchesToFeet;
import static utils.UnitConverter.inchesToPixels;
import static utils.UnitConverter.rotateRobotToCartesian;

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

    public GraphingUtil(ArrayList<Point> controlPoints, ArrayList<Point> path, ArrayList<PointRow> rows,
                        Polyline polyPos, Polyline polyLeft, Polyline polyRight,
                        LineChart<Double, Double> chtLeft, LineChart<Double, Double> chtRight, LineChart<Double, Double> chtCenter,
                        Tab tabVel) {
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
    }

    public void updatePolyline() {
        controlPoints = (ArrayList<Point>) rows.stream()
                .sorted(comparingInt(PointRow::getIndex))
                .map(PointRow::getPoint)
                .collect(toList());

        path = Bezier.generateAll(controlPoints);

        polyPos.getPoints().clear();
        path.forEach(point -> polyPos.getPoints().addAll(point.getXPixels(), imageHeight() - point.getYPixels()));

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

    private void pathFromPath() {
        final double dist = width() / 2;
        for (Point point : path) {
            double angle = rotateRobotToCartesian(Math.toRadians(point.getHeading()));
            double offsetX = inchesToPixels(dist * Math.sin(angle)),
                    offsetY = inchesToPixels(dist * Math.cos(angle));
            polyLeft.getPoints().addAll(point.getXPixels() - offsetX,
                    imageHeight() - (point.getYPixels() + offsetY));
            polyRight.getPoints().addAll(point.getXPixels() + offsetX,
                    imageHeight() - (point.getYPixels() - offsetY));
        }
    }

    private void pathFromVel() {
        final double dist = width() / 2;

        double angle = rotateRobotToCartesian(Math.toRadians(path.get(0).getHeading()));
        double offsetX = inchesToPixels(dist * Math.sin(angle)),
                offsetY = inchesToPixels(dist * Math.cos(angle));

        double xl = path.get(0).getXPixels() - offsetX,
                yl = imageHeight() - (path.get(0).getYPixels() + offsetY),
                xr = path.get(0).getXPixels() + offsetX,
                yr = imageHeight() - (path.get(0).getYPixels() - offsetY);
        polyLeft.getPoints().addAll(xl, yl);
        polyRight.getPoints().addAll(xr, yr);

        for (Point point : path) {
            double angBetween = rotateRobotToCartesian(Math.toRadians((new Point(xl, yl).angleTo(new Point(xr, yr)))));
            xl += pointDistFromVel(point.getLeftVelLinearFeet(), angBetween, Math::sin);
            yl -= pointDistFromVel(point.getLeftVelLinearFeet(), angBetween, Math::cos);
            polyLeft.getPoints().addAll(xl, yl);
            xr += pointDistFromVel(point.getRightVelLinearFeet(), angBetween, Math::sin);
            yr -= pointDistFromVel(point.getRightVelLinearFeet(), angBetween, Math::cos);
            polyRight.getPoints().addAll(xr, yr);
        }
    }

    private double pointDistFromVel(double linearVelFeet, double angleBetween, Function<Double, Double> cosOrSin) {
//        double angle = rotateRobotToCartesian(Math.toRadians(point.getHeading()));
        double distTraveled = feetToPixels(linearVelFeet * timeStep());
        return cosOrSin.apply(angleBetween) * distTraveled;
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
        double totalDist = 0;
        for (int i = 0; i < path.size(); i++) {
            double curTime = path.get(path.size() - 1).getTime() * i / path.size();
            double curLeftAccel = i == 0 ? 0 : path.get(i).getLeftVelLinearFeet() - path.get(i - 1).getLeftVelLinearFeet();
            double curRightAccel = i == 0 ? 0 : path.get(i).getRightVelLinearFeet() - path.get(i - 1).getRightVelLinearFeet();
            double curCenterAccel = i == 0 ? 0 : path.get(i).getTargetVelocity() - path.get(i - 1).getTargetVelocity();
            totalDist += path.get(i).getTargetVelocity() * timeStep();
            leftPos.getData().add(new XYChart.Data<>(curTime, inchesToFeet(path.get(i).getLeftPos())));
            leftVel.getData().add(new XYChart.Data<>(curTime, path.get(i).getLeftVelLinearFeet()));
            leftAccel.getData().add(new XYChart.Data<>(curTime, Utils.absRange(curLeftAccel / timeStep(), 0, maxAccel())));
            rightPos.getData().add(new XYChart.Data<>(curTime, inchesToFeet(path.get(i).getRightPos())));
            rightVel.getData().add(new XYChart.Data<>(curTime, path.get(i).getRightVelLinearFeet()));
            rightAccel.getData().add(new XYChart.Data<>(curTime, Utils.absRange(curRightAccel / timeStep(), 0, maxAccel())));
            centerPos.getData().add(new XYChart.Data<>(curTime, totalDist));
            centerVel.getData().add(new XYChart.Data<>(curTime, path.get(i).getTargetVelocity()));
            centerAccel.getData().add(new XYChart.Data<>(curTime, Utils.absRange(curCenterAccel / timeStep(), 0, maxAccel())));
        }
        chtLeft.getData().addAll(leftPos, leftVel, leftAccel);
        chtRight.getData().addAll(rightPos, rightVel, rightAccel);
        chtCenter.getData().addAll(centerPos, centerVel, centerAccel);
    }

}
