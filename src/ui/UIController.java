package ui;

import bezier.Bezier;
import bezier.Point;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import utils.CSVWriter;
import utils.Config;
import utils.UnitConverter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

public class UIController {
    @FXML
    private Tab tabVel;

    @FXML
    private Pane root;

    @FXML
    private Circle cursorHighlight,
            pointHighlight;

    @FXML
    private Polyline polyPos, polyLeft, polyRight;

    @FXML
    private LineChart<Double, Double> chtLeft, chtRight, chtCenter;

    @FXML
    private ImageView imgField;

    @FXML
    private GridPane grdPoints;

    //config values
    @FXML
    private TextField cfgRadius,
            cfgWidth,
            cfgLength,
            cfgMaxVel,
            cfgMaxAccel,
            cfgJerk,
            cfgTimeStep,
            cfgPathName;

    @FXML
    private CheckBox cfgDrawWheels;

    private static Image backgroundImage = new Image("images/FRC2018.png");
    private ArrayList<Point> controlPoints, path;
    private ArrayList<ArrayList<PointRow>> previousStates; //for undo/redo
    private ArrayList<PointRow> rows;
    private int nextIndex,
            gridDnDIndex = -1, //-1 means nothing is being dragged
            dragStartIndex,
            currentState;
    private PointRow draggedRow;
    private static Config config;

    @FXML
    private void initialize() {
        config = new Config("src/config.properties", cfgDrawWheels, cfgLength, cfgMaxAccel, cfgMaxVel, cfgJerk,
                cfgRadius, cfgWidth, cfgTimeStep, cfgPathName);

//        backgroundImage = new Image(Config.getStringProperty("img_path", "src\\images\\FRC2018.png"));
        imgField.setImage(backgroundImage);
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
        tabVel.setOnSelectionChanged(event -> graphMotion());
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
                handleComboResults(row.getComboBox().getValue(), row.getIndex());
                ((ComboBox) node).getSelectionModel().clearSelection();
                imgField.requestFocus(); //to prevent double selection
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
        switch (result) {   //can't actually be null
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

        path = Bezier.generateAll(controlPoints);

        polyPos.getPoints().clear();
        path.forEach(point -> polyPos.getPoints().addAll(point.getX(), imageHeight() - point.getY()));

        graphMotion();

        polyLeft.getPoints().clear();
        polyRight.getPoints().clear();

        if (Config.getBooleanProperty("draw_wheels")) {
            final double dist = UnitConverter.inchesToPixels(Config.getDoubleProperty("width") / 2);
            for (Point point : path) {
                double angle = UnitConverter.rotateRobotToCartesian(Math.toRadians(point.getHeading()));
                polyLeft.getPoints().addAll(point.getX() - dist * Math.sin(angle),
                        imageHeight() - (point.getY() + dist * Math.cos(angle)));
                polyRight.getPoints().addAll(point.getX() + dist * Math.sin(angle),
                        imageHeight() - (point.getY() - dist * Math.cos(angle)));
            }
        }

//        final double dist = UnitConverter.inchesToPixels(Config.getDoubleProperty("width") / 2);
//        polyLeft.getPoints().addAll(path.stream()
//                .map(p -> p.getX() - dist * Math.cos(UnitConverter.rotateRobotToCartesian(Math.toRadians(p.getHeading()))))
//                .collect(Collectors.toList()));

        if (polyPos.getPoints().isEmpty())         //polyline with no points doesn't redraw
            polyPos.getPoints().addAll(0.0, 0.0);  //so this does
        if (polyLeft.getPoints().isEmpty())
            polyLeft.getPoints().addAll(0.0, 0.0);
        if (polyRight.getPoints().isEmpty())
            polyRight.getPoints().addAll(0.0, 0.0);
    }

    private void graphMotion() {
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
        leftPos.setName("pos");leftVel.setName("vel");leftAccel.setName("accel");
        rightPos.setName("pos");rightVel.setName("vel");rightAccel.setName("accel");
        centerPos.setName("pos");centerVel.setName("vel");centerAccel.setName("accel");
        double totalDist = 0;
        for (int i = 0; i < path.size(); i++) {
            double curTime = path.get(path.size() - 1).getTime() * i / path.size();
            double curLeftAccel = i == 0 ? 0 : path.get(i).getLeftVelLinearFeet() - path.get(i - 1).getLeftVelLinearFeet();
            double curRightAccel = i == 0 ? 0 : path.get(i).getRightVelLinearFeet() - path.get(i - 1).getRightVelLinearFeet();
            double curCenterAccel = i == 0 ? 0 : path.get(i).getTargetVelocity() - path.get(i - 1).getTargetVelocity();
            totalDist += path.get(i).getTargetVelocity() * Config.getDoubleProperty("time_step");
            leftPos.getData().add(new XYChart.Data<>(curTime, UnitConverter.inchesToFeet(path.get(i).getLeftPos())));
            leftVel.getData().add(new XYChart.Data<>(curTime, path.get(i).getLeftVelLinearFeet()));
            leftAccel.getData().add(new XYChart.Data<>(curTime, curLeftAccel / Config.getDoubleProperty("time_step")));
            rightPos.getData().add(new XYChart.Data<>(curTime, UnitConverter.inchesToFeet(path.get(i).getRightPos())));
            rightVel.getData().add(new XYChart.Data<>(curTime, path.get(i).getRightVelLinearFeet()));
            rightAccel.getData().add(new XYChart.Data<>(curTime, curRightAccel / Config.getDoubleProperty("time_step")));
            centerPos.getData().add(new XYChart.Data<>(curTime, totalDist));
            centerVel.getData().add(new XYChart.Data<>(curTime, path.get(i).getTargetVelocity()));
            centerAccel.getData().add(new XYChart.Data<>(curTime, curCenterAccel / Config.getDoubleProperty("time_step")));
        }
        chtLeft.getData().addAll(leftPos, leftVel, leftAccel);
        chtRight.getData().addAll(rightPos, rightVel, rightAccel);
        chtCenter.getData().addAll(centerPos, centerVel, centerAccel);
    }

    @FXML
    private void deleteLastPoint() {
        deletePoints(rows.size() - 1, rows.size() - 1);
    }

    @FXML
    private void deleteAllPoints() {
        deletePoints(0, rows.size() - 1);
    }

    /**
     * deletes a number of PointRows equal to endIndex - startIndex + 1.
     * <p>to delete one point, startIndex and endIndex should be equal</p>
     */
    private void deletePoints(int startIndex, int endIndex) {
        for (int i = endIndex; i >= startIndex; i--) {
            PointRow currentRow = rowAtIndex(i);
            grdPoints.getChildren().removeAll(currentRow.getAllNodes());
            rows.remove(currentRow);
        }
        rows.stream().
                filter(row -> row.getIndex() > endIndex).
                forEach(row -> row.moveIndex(endIndex - startIndex + 1));
        updatePolyline();
        addSavedState(rows);
    }

    private PointRow rowAtIndex(int index) {
        for (PointRow row : rows) {
            if (row.getIndex() == index)
                return row;
        }
        throw new RuntimeException();
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
//        String imgDir = Config.getStringProperty("img_path", "src\\images");
//        if (imgDir.contains("."))
//            imgDir = imgDir.substring(0, imgDir.indexOf("."));
//        fileChooser.setInitialDirectory(new File(imgDir));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Images", "*.jpg", "*.png", "*.jpeg", "*.gif", "*.bmp", "*.pdn"),
                new FileChooser.ExtensionFilter("JPG", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("PNG", "*.png")
        );
        File chosenImage = fileChooser.showOpenDialog(root.getScene().getWindow());
//        config.setProperty("img_path", chosenImage.getAbsolutePath());
        backgroundImage = new Image(chosenImage.toURI().toURL().toString());
        imgField.setImage(backgroundImage);
        imgField.setFitWidth(imageWidth());
        imgField.setFitHeight(imageHeight());
    }

    @FXML
    private void mnuExport() { //TODO make a temporary backup when doing this
        String url = Config.getStringProperty("csv_out_dir") + "\\" + Config.getStringProperty("path_name");
        try (CSVWriter leftWriter = new CSVWriter(url + "_left.csv");
             CSVWriter rightWriter = new CSVWriter(url + "_right.csv")) {
            leftWriter.writePoints("Dist,Vel,Heading", path,
                    point -> String.valueOf(point.getLeftPos()),
                    point -> String.valueOf(point.getLeftVel()),
                    point -> String.valueOf(point.getHeading()));
            rightWriter.writePoints("Dist,Vel,Heading", path,
                    point -> String.valueOf(point.getRightPos()),
                    point -> String.valueOf(point.getRightVel()),
                    point -> String.valueOf(point.getHeading()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static double imageHeight() {
        return backgroundImage.getHeight();
    }

    public static double imageWidth() {
        return backgroundImage.getWidth();
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

    @FXML
    private void configUpdate() {
        config.updateConfig();
    }

    @FXML
    private void mnuChangeCSVOut() {
        configUpdate();
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("CSV Generation Location");
        dirChooser.setInitialDirectory(new File(Config.getStringProperty("csv_out_dir", "src")));
        File dir = dirChooser.showDialog(root.getScene().getWindow());
        config.setProperty("csv_out_dir", dir.getAbsolutePath());
    }
}

