package ui;

import bezier.Bezier;
import bezier.Point;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;

public class UIController {

    @FXML
    private Pane root;

    @FXML
    private Circle cursorHighlight,
            pointHighlight;

    @FXML
    private Polyline polyline;

    @FXML
    private ImageView imgField;

    @FXML
    private GridPane grdPoints;

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
        imgField.setFitWidth(imageWidth());
        imgField.setFitHeight(imageHeight());
        controlPoints = new ArrayList<>();
        path = new ArrayList<>();
        rows = new ArrayList<>();
        previousStates = new ArrayList<>();
        setNextIndex(-1);
        grdPoints.setOnDragOver(event -> {
            if (gridDnDIndex == -1) return;
            double y = event.getY(),
                rowHeight = grdPoints.getVgap() + rows.get(0).getxText().getHeight();
            gridDnDIndex = (int) Math.floor(y / rowHeight);
            dndHandling(draggedRow, false);
            updatePolyline();
        });
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

    private void addSavedState(ArrayList<PointRow> rows) {
        if (currentState != previousStates.size() - 1) {
            previousStates.removeIf(pointRows -> previousStates.indexOf(pointRows) > currentState);
        }
        previousStates.add(new ArrayList<>());
        rows.forEach(row -> previousStates.get(previousStates.size() - 1).add(new PointRow(row)));
        previousStates.get(previousStates.size() - 1).forEach(row -> row.getAllNodes().forEach(node -> pointRowListeners(node, row)));
        currentState = previousStates.size() - 1;
    }

    private void handleComboResults(String result, int index) {
        if (nextIndex != -1)
            return;
        if (result.equals(PointMenuResult.DELETE_POINT.toString())) {
            deletePoints(index, index);
        } else if (result.equals(PointMenuResult.POINT_EDIT_MODE.toString())) {
            setNextIndex(index);
            pointHighlight.setCenterX(rows.get(index).getXValue());
            pointHighlight.setCenterY(imageHeight() - rows.get(index).getYValue());
        } else if (result.equals(PointMenuResult.REORDER_POINT.toString())) {
            System.out.println("NO");
        }
    }

    private void pointRowListeners(Node node, PointRow row) {
        if (node instanceof TextField)
            node.setOnKeyReleased(event -> {
                row.getPoint().setX(row.getXValue());
                row.getPoint().setY(row.getYValue());
                addSavedState(rows);
                updatePolyline();
            });
        if (node instanceof CheckBox)
            ((CheckBox) node).setOnAction(event -> {
                row.getPoint().setIntercept(((CheckBox) node).isSelected());
                addSavedState(rows);
                updatePolyline();
            });
        if (node instanceof ComboBox)
            ((ComboBox) node).setOnAction(event -> {
                handleComboResults(row.getComboBox().getValue().trim(), row.getIndex());
                imgField.requestFocus();
            });

        //Drag and Drop listeners
        node.setOnDragDetected(event -> {
            gridDnDIndex = row.getIndex();
            draggedRow = row;
            dragStartIndex = row.getIndex();
            Dragboard db = node.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString("this has to exist or nothing works! ):");
            db.setContent(content);
        });
        node.setOnDragDone(event -> {
            dndHandling(row, draggedRow.getIndex() != dragStartIndex);
            updatePolyline();
        });
    }

    private void updatePolyline() {
        controlPoints.clear();
        rows.forEach(row -> controlPoints.add(row.getPoint()));
        rows.forEach(row -> controlPoints.set(row.getIndex(), row.getPoint()));

        path = Bezier.generate(controlPoints);

        polyline.getPoints().clear();
        path.forEach(point -> polyline.getPoints().addAll(point.getX(), imageHeight() - point.getY()));

        if (polyline.getPoints().isEmpty())         //polyline with no points doesn't redraw
            polyline.getPoints().addAll(0.0, 0.0);  //so this does
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
        boolean intercept = mouseEvent.getButton() == MouseButton.PRIMARY;
        if (x < 0 || y < 0 || x > imageWidth() || y > imageHeight())
            return;
        y = imageHeight() - y;
        if (nextIndex == -1) {
            addNewPointRow(String.valueOf(x), String.valueOf(y), intercept);
        } else {
            rows.get(nextIndex).setPoint(new Point(x, y, rows.get(nextIndex).getCheckValue()));
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
        if (nextIndex == -1) return;
        PointRow row = rows.get(nextIndex);
        imgField.requestFocus();
        boolean ctrl = keyEvent.isControlDown(),
                shift = keyEvent.isShiftDown();
        int change;
        change = shift ? ctrl ? 20 : 1 : ctrl ? 10 : 5;    //shift = 1      ctrl = 10       both = 20
        double x = row.getXValue();
        double y = row.getYValue();
        KeyCode key = keyEvent.getCode();
        switch (key) {
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
        row.setPoint(new Point(x, y, row.getCheckValue()));
        updatePolyline();
        pointHighlight.setCenterX(x);
        pointHighlight.setCenterY(imageHeight() - y);
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
    private void mnuExport() { //TalonSRX uses double heading(deg), double position, double velocity to csv
        //TODO export
    }

    //TODO auto generate 90 degree curves etc.

    public static double imageHeight() {
        return fieldImage.getHeight();
    }

    public static double imageWidth() {
        return fieldImage.getWidth();
    }

    private double[] toRealCoords(double x, double y) {
        int FIELD_WIDTH_INCHES = 324;
        int FIELD_LENGTH_INCHES = 384;

        y = imageHeight() - y;
        x /= imageWidth();
        x *= FIELD_WIDTH_INCHES;
        y /= imageHeight();
        y *= FIELD_LENGTH_INCHES;
        return new double[]{x, y};
    }

    @FXML
    private void undo() {
        if (currentState == 0)
            return;
        grdPoints.getChildren().clear();
        rows.clear();
        currentState -= 1;
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
        currentState += 1;
        for (PointRow row : previousStates.get(currentState)) {
            row.updatePoint();
            rows.add(row);
            grdPoints.getChildren().addAll(row.getAllNodes());
            row.getAllNodes().forEach(node -> GridPane.setRowIndex(node, row.getIndex()));
        }
        updatePolyline();
    }
}

