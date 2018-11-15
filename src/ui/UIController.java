package ui;

import bezier.Bezier;
import bezier.Point;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.stage.FileChooser;
import utils.CSVWriter;
import utils.UnitConverter;
import utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class UIController {
    @FXML
    private Tab tabVel;

    @FXML
    private Pane root;

    @FXML
    private Circle cursorHighlight,
            pointHighlight;

    @FXML
    private Polyline polyPos, polyLeft, polyRight; //TODO add extrapolated wheel lines

    @FXML
    private LineChart<Double, Double> chtVel; //TODO add accel chart

    @FXML
    private ImageView imgField;

    @FXML
    private GridPane grdPoints;

    @FXML
    private TextField cfgRadius,
            cfgWidth,
            cfgLength,
            cfgMaxVel,
            cfgMaxAccel,
            cfgTime;

    @FXML
    private CheckBox cfgDrawWheels;
    //TODO make a config file

    private static Image fieldImage = new Image("images/FRC2018.png");
    private ArrayList<Point> controlPoints, path;
    private ArrayList<ArrayList<PointRow>> previousStates; //for undo/redo
    private ArrayList<PointRow> rows;
    private int nextIndex,
            gridDnDIndex = -1, //-1 means nothing is being dragged
            dragStartIndex,
            currentState;
    private PointRow draggedRow;

    @FXML
    private void initialize() {
        imgField.setImage(fieldImage);
//        imgField.setFitWidth(imageWidth());
//        imgField.setFitHeight(imageHeight());
        controlPoints = new ArrayList<>();
        path = new ArrayList<>();
        rows = new ArrayList<>();
        previousStates = new ArrayList<>();
        setNextIndex(-1);
        grdPoints.setOnDragOver(event -> {
            if (gridDnDIndex == -1) return;
            double y = event.getY(),
                    rowHeight = grdPoints.getVgap() + rows.get(0).getComboBox().getHeight();
            gridDnDIndex = (int) Math.floor(y / rowHeight);
            dndHandling(draggedRow, false);
            updatePolyline();
        });
        tabVel.setOnSelectionChanged(event -> graphVels());
    }

    @FXML
    private void btnNewPointEvent() {
        addNewPointRow("", "", false);
    }

    private void addNewPointRow(String x, String y, boolean intercept) {
        PointRow row = new PointRow(rows.size());
        row.makeAllNodes(new Point(x, y, intercept));
        row.getAllNodes().forEach(node -> pointRowListeners(node, row));
        rows.add(row);
        addSavedState(rows);
        grdPoints.getChildren().addAll(row.getAllNodes());
    }

    private void addSavedState(ArrayList<PointRow> rows) {
        if (currentState != previousStates.size() - 1) {
            previousStates.removeIf(pointRows -> previousStates.indexOf(pointRows) > currentState);
        }
        previousStates.add(new ArrayList<>());
        rows.forEach(row -> previousStates.get(previousStates.size() - 1).add(new PointRow(row)));
        previousStates.get(previousStates.size() - 1).forEach(row -> row.getAllNodes().forEach(node -> pointRowListeners(node, row)));
        currentState = previousStates.size() - 1;
    }

    private void pointRowListeners(Node node, PointRow row) {
        if (node instanceof TextField)
            node.setOnKeyReleased(event -> {
                row.updatePoint();
                addSavedState(rows);
                updatePolyline();
            });
        if (node instanceof CheckBox)
            ((CheckBox) node).setOnAction(event -> {
                row.updatePoint();
                addSavedState(rows);
                updatePolyline();
            });
        if (node instanceof ComboBox)
            ((ComboBox) node).setOnAction(event -> {
//                row.updatePoint();
                handleComboResults(row.getComboBox().getValue(), row.getIndex());
                ((ComboBox) node).getSelectionModel().clearSelection();
                imgField.requestFocus();
            });

        //Drag and Drop listeners
        node.setOnDragDetected(event -> {
            gridDnDIndex = row.getIndex();
            draggedRow = row;
            dragStartIndex = row.getIndex();
            Dragboard db = node.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString("this has to exist or nothing works! :( "); //<---- self documenting code
            db.setContent(content);
        });
        node.setOnDragDone(event -> {
            dndHandling(row, draggedRow.getIndex() != dragStartIndex);
            updatePolyline();
        });
    }

    private void handleComboResults(String res, int index) {
        if (nextIndex != -1)
            return;
        PointMenuResult result = null;
        for (PointMenuResult r : PointMenuResult.values()) {
            if (r.toString().equals(res)) result = r;
        }
        assert result != null;
        switch (result) {
            case DELETE_POINT:
                deletePoints(index, index);
                break;
            case POINT_EDIT_MODE:
                setNextIndex(index);
                pointHighlight.setCenterX(rows.get(index).getXValue());
                pointHighlight.setCenterY(imageHeight() - rows.get(index).getYValue());
                break;
            case TOGGLE_OVERRIDE_VEL:
                rows.get(index).getPoint().toggleOverride();
                addSavedState(rows);
                updatePolyline();
                break;
        }
    }

    private void dndHandling(PointRow draggedRow, boolean save) {
        for (PointRow r : rows) {
            if (gridDnDIndex < draggedRow.getIndex()) {
                if (r.getIndex() >= gridDnDIndex && r.getIndex() < draggedRow.getIndex()) {
                    r.moveIndex(-1);
                }
            } else if (gridDnDIndex > draggedRow.getIndex()) {
                if (r.getIndex() <= gridDnDIndex && r.getIndex() > draggedRow.getIndex()) {
                    r.moveIndex(1);
                }
            }
        }
        draggedRow.setIndex(gridDnDIndex);
        if (save) addSavedState(rows);
    }

    private void updatePolyline() {
        controlPoints.clear();
        rows.forEach(row -> controlPoints.add(row.getPoint()));
        rows.forEach(row -> controlPoints.set(row.getIndex(), row.getPoint()));

        path = Bezier.generate(controlPoints, Utils.parseDouble(cfgMaxVel.getText().trim()),
                Utils.parseDouble(cfgMaxAccel.getText().trim()), 15);
        Bezier.motion(path, Utils.parseDouble(cfgLength.getText().trim()));

        polyPos.getPoints().clear();
        path.forEach(point -> polyPos.getPoints().addAll(point.getX(), imageHeight() - point.getY()));

        graphVels();

        polyLeft.getPoints().clear();
        polyRight.getPoints().clear();

        if (cfgDrawWheels.isSelected()) {
            final double dist = UnitConverter.inchesToPixels(Utils.parseDouble(cfgWidth.getText()) / 2);
            for (Point point : path) {
                double angle = UnitConverter.rotateAngle(Math.toRadians(-point.getHeading()), 90);
                polyLeft.getPoints().addAll(point.getX() - dist * Math.cos(angle),
                        imageHeight() - (point.getY() + dist * Math.sin(angle)));
                polyRight.getPoints().addAll(point.getX() + dist * Math.cos(angle),
                        imageHeight() - (point.getY() - dist * Math.sin(angle)));
            }
        }

        if (polyPos.getPoints().isEmpty())         //polyline with no points doesn't redraw
            polyPos.getPoints().addAll(0.0, 0.0);  //so this does
        if (polyLeft.getPoints().isEmpty())
            polyLeft.getPoints().addAll(0.0, 0.0);
        if (polyRight.getPoints().isEmpty())
            polyRight.getPoints().addAll(0.0, 0.0);

//        double leftX = path.get(0).getX() + -1/Math.cos(toCoordAngle.apply(path.get(0).getHeading())) * dist,
//                leftY = path.get(0).getY() + -1/Math.sin(toCoordAngle.apply(path.get(0).getHeading())) * dist;
        //        for (Point p : path) {
//            polyLeft.getPoints().addAll(leftX, imageHeight() - leftY);
//            leftX += p.getTargetVelocity() * Math.cos(toCoordAngle.apply(p.getHeading()));
//            leftY += p.getTargetVelocity() * Math.sin(toCoordAngle.apply(p.getHeading()));
//        }
    }

    private void graphVels() {
        if (!tabVel.isSelected()) return;
        chtVel.getData().clear();
        XYChart.Series<Double, Double> points = new XYChart.Series<>();
        for (int i = 0; i < path.size(); i++) {
            points.getData().add(new XYChart.Data<>(15.0 * i / path.size(), path.get(i).getTargetVelocity()));
        }
        chtVel.getData().add(points);
    }

    @FXML
    private void deleteLastPoint() {
        deletePoints(rows.size() - 1, rows.size() - 1);
    }

    @FXML
    private void deleteAllPoints() {
        deletePoints(0, rows.size() - 1);
    }

    private void deletePoints(int startIndex, int endIndex) {
        for (int i = endIndex; i >= startIndex; i--) {
            grdPoints.getChildren().removeAll(rows.get(i).getAllNodes());
            rows.remove(i);
        }
        rows.stream().
                filter(row -> row.getIndex() > endIndex).
                forEach(row -> row.moveIndex(endIndex - startIndex + 1));
        updatePolyline();
        addSavedState(rows);
    }

    @FXML
    private void clickEvent(MouseEvent mouseEvent) {
        double x = mouseEvent.getX(),
                y = mouseEvent.getY();
        boolean intercept = mouseEvent.getButton() == MouseButton.PRIMARY && !mouseEvent.isControlDown();
        if (x < 0 || y < 0 || x > imageWidth() || y > imageHeight())
            return;
        y = imageHeight() - y;
        if (nextIndex == -1) {
            addNewPointRow(String.valueOf(x), String.valueOf(y), intercept);
        } else {
            rows.get(nextIndex).setPoint(new Point(x, y, rows.get(nextIndex).getInterceptValue()));
            addSavedState(rows);
            setNextIndex(-1);
        }
        updatePolyline();
    }

    @FXML
    private void mouseMoveEvent(MouseEvent event) {
        cursorHighlight.setCenterX(Math.max(0, Math.min(imageWidth(), event.getX())));
        cursorHighlight.setCenterY(Math.max(0, Math.min(imageHeight(), event.getY())));
    }

    @FXML
    private void angles() {
        double total = 0,
                currAngle;
        for (int i = 0; i < path.size() - 1; i++) {
            currAngle = path.get(i).distanceTo(path.get(i + 1));
            total += currAngle;
        }
        System.out.println(total / path.size());
    }

    @FXML
    private void keyReleasedEvent(KeyEvent keyEvent) {
        if (nextIndex != -1) {
            PointRow row = rows.get(nextIndex);
            imgField.requestFocus();
            boolean ctrl = keyEvent.isControlDown(),
                    shift = keyEvent.isShiftDown();
            int change;
            change = shift ? ctrl ? 20 : 1 : ctrl ? 10 : 5;    //shift = 1      ctrl = 10       both = 20
            double x = row.getXValue();
            double y = row.getYValue();
            switch (keyEvent.getCode()) {
                case UP:
                    y += change;
                    break;
                case DOWN:
                    y -= change;
                    break;
                case LEFT:
                    x -= change;
                    break;
                case RIGHT:
                    x += change;
                    break;
                case ENTER:
                    setNextIndex(-1);
                    addSavedState(rows);
                    break;
                case ESCAPE:
                    setNextIndex(-1);
                    break;
            }
            row.setPoint(new Point(x, y, row.getInterceptValue()));
            updatePolyline();
            pointHighlight.setCenterX(x);
            pointHighlight.setCenterY(imageHeight() - y);
        } else {
            boolean pointsFocused = false;
            int focusedIndex = 0, focusedColumn = 0;
            for (PointRow row : rows) {
                for (int i = 0; i < row.getAllNodes().size(); i++) {
                    if (row.getAllNodes().get(i).isFocused()) {
                        pointsFocused = true;
                        focusedIndex = row.getIndex();
                        focusedColumn = i;
                    }
                }
            }
            if (!pointsFocused) return;
            switch (keyEvent.getCode()) {
                case UP:
                    rows.get(Math.max(0, focusedIndex - 1)).getAllNodes().get(focusedColumn).requestFocus();
                    break;
                case DOWN:
                    rows.get(Math.min(rows.size() - 1, focusedIndex + 1)).getAllNodes().get(focusedColumn).requestFocus();
                    break;
//                case LEFT:
//                    rows.get(focusedIndex).getAllNodes().get(Math.max(0, focusedColumn - 1)).requestFocus();
//                    break;
//                case RIGHT:
//                    rows.get(focusedIndex).getAllNodes().get(Math.min(rows.get(0).getAllNodes().size() - 1, focusedColumn + 1)).requestFocus();
//                    break;
            }
        }
    }

    private void setNextIndex(int nextIndex) {
        this.nextIndex = nextIndex;
        pointHighlight.setVisible(nextIndex != -1);
        cursorHighlight.setVisible(nextIndex != -1);
    }

    @FXML
    private void mnuOpenImage() throws MalformedURLException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Field Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Images", "*.jpg", "*.png", "*.jpeg", "*.gif", "*.bmp", "*.pdn"),
                new FileChooser.ExtensionFilter("JPG", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("PNG", "*.png")
        );
        File chosenImage = fileChooser.showOpenDialog(root.getScene().getWindow());
        fieldImage = new Image(chosenImage.toURI().toURL().toString());
        imgField.setImage(fieldImage);
        imgField.setFitWidth(imageWidth());
        imgField.setFitHeight(imageHeight());
    }

    @FXML
    private void mnuExport() { //TalonSRX uses position, velocity to csv
//        File file = new File("src/csv/path.csv");
        Bezier.motion(path, Utils.parseDouble(cfgWidth.getText()));
        try (CSVWriter leftWriter = new CSVWriter("src/csv/leftpath.csv");
             CSVWriter rightWriter = new CSVWriter("src/csv/rightpath.csv")) {
            leftWriter.writePoints("Dist,Vel,Heading,Last", path,
                    point -> String.valueOf(point.getLeftPos()), //TODO implement position
                    point -> String.valueOf(point.getLeftVel()),
                    point -> String.valueOf(point.getHeading()),
                    point -> String.valueOf(point.isLast()));
            rightWriter.writePoints("Dist,Vel,Heading,Last", path,
                    point -> String.valueOf(point.getRightPos()), //TODO implement position
                    point -> String.valueOf(point.getRightVel()),
                    point -> String.valueOf(point.getHeading()),
                    point -> String.valueOf(point.isLast()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void mnuExportCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("private RobotGrid SPLINENAME() {\n");
        sb.append("\tRobotGrid grid = new RobotGrid(");

        ArrayList<Point> intercepts = (ArrayList<Point>) controlPoints.stream().filter(Point::isIntercept).collect(Collectors.toList());
        for (Point p : intercepts) {

        }

        for (int i = 0; i < controlPoints.size(); i++) {
//            if ()
        }
    }

    //TODO auto generate 90 degree curves etc. maybe

    public static double imageHeight() {
        return fieldImage.getHeight();
    }

    public static double imageWidth() {
        return fieldImage.getWidth();
    }

    @FXML
    private void undo() {
        if (currentState == 0)
            return;
        grdPoints.getChildren().clear();
        rows.clear();
        currentState--;
        for (PointRow row : previousStates.get(currentState)) {
            row.updatePoint();
            rows.add(row);
            grdPoints.getChildren().addAll(row.getAllNodes());
            row.getAllNodes().forEach(node -> GridPane.setRowIndex(node, row.getIndex()));
        }
        updatePolyline();
    }

    @FXML
    private void redo() {
        if (currentState == previousStates.size() - 1)
            return;
        grdPoints.getChildren().clear();
        rows.clear();
        currentState++;
        for (PointRow row : previousStates.get(currentState)) {
            row.updatePoint();
            rows.add(row);
            grdPoints.getChildren().addAll(row.getAllNodes());
            row.getAllNodes().forEach(node -> GridPane.setRowIndex(node, row.getIndex()));
        }
        updatePolyline();
    }

    public void mnuImportCode(ActionEvent event) {
    }
}

