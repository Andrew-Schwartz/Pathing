package ui;

import bezier.Point;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import utils.CSVWriter;
import utils.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;

import static utils.UnitConverter.pixelsToInches;

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
    private ComboBox<String> cfgDrawWheelType;

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
    GraphingUtil graph;

    @FXML
    private void initialize() {
        cfgDrawWheelType.setItems(FXCollections.observableArrayList(
                Arrays.stream(WheelPathType.values())
                        .map(WheelPathType::toString)
                        .toArray(String[]::new)
        ));
        config = new Config("saves/config.properties", cfgDrawWheelType, cfgLength, cfgMaxAccel, cfgMaxVel, cfgJerk,
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
            graph.updatePolyline();
        });
        tabVel.setOnSelectionChanged(event -> graph.graphMotion());
        graph = new GraphingUtil(controlPoints, path, rows,
                polyPos, polyLeft, polyRight,
                chtLeft, chtRight, chtCenter,
                tabVel);
    }

    @FXML
    private void btnNewPointEvent() {
        addNewPointRow(new Point(0, 0), true);
    }

    private void addNewPointRow(Point point, boolean save) {
        var row = new PointRow(rows.size(), point);
        row.getAllNodes().forEach(node -> pointRowListeners(node, row));
        rows.add(row);
        if (save) addSavedState();
        grdPoints.getChildren().addAll(row.getAllNodes());
    }

    private void addSavedState() {
        if (currentState != previousStates.size() - 1)
            previousStates.removeIf(pointRows -> previousStates.indexOf(pointRows) > currentState);
        previousStates.add(new ArrayList<>());
        rows.forEach(row -> previousStates.get(previousStates.size() - 1).add(new PointRow(row.getIndex(), row.getPoint())));
        previousStates.get(previousStates.size() - 1)
                .forEach(row -> row.getAllNodes()
                        .forEach(node -> pointRowListeners(node, row)));
        currentState = previousStates.size() - 1;
    }

    private void pointRowListeners(Node node, PointRow row) {
        if (node instanceof TextField)
            node.setOnKeyReleased(event -> {
                row.updatePoint();
                addSavedState();
                graph.updatePolyline();
            });
        if (node instanceof CheckBox)
            ((CheckBox) node).setOnAction(event -> {
                row.updatePoint();
                addSavedState();
                graph.updatePolyline();
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
            graph.updatePolyline();
        });
    }

    private void handleComboResults(String res, int index) {
        if (nextIndex != -1)
            return;
        PointMenuResult result = Arrays.stream(PointMenuResult.values())
                .filter(pmr -> pmr.toString().equals(res))
                .findFirst()
                .orElseThrow();

        switch (result) {
            case MENU:
                var menu = MenuFactory.menu(rows.get(index).getPoint());
                menu.showAndWait().ifPresent(rows.get(index)::setPoint);
                addSavedState();
                graph.updatePolyline();
                break;
            case DELETE_POINT:
                deletePoints(index, index);
                break;
            case POINT_EDIT_MODE:
                setNextIndex(index);
                pointHighlight.setCenterX(rows.get(index).getPoint().getXPixels());
                pointHighlight.setCenterY(imageHeight() - rows.get(index).getPoint().getYPixels());
                break;
            case TOGGLE_OVERRIDE_VEL:
                rows.get(index).getPoint().setOverrideMaxVel(!rows.get(index).getPoint().isOverrideMaxVel());
                addSavedState();
                graph.updatePolyline();
                break;
            case TOGGLE_BACKWARDS:
                rows.get(index).getPoint().setReverse(!rows.get(index).getPoint().isReverse());
                addSavedState();
                graph.updatePolyline();
                break;
            default:
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
        if (save) addSavedState();
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
        if (nextIndex >= startIndex && nextIndex <= endIndex) setNextIndex(-1);
        for (int i = endIndex; i >= startIndex; i--) {
            PointRow currentRow = rowAtIndex(i);
            grdPoints.getChildren().removeAll(currentRow.getAllNodes());
            rows.remove(currentRow);
        }
        rows.stream()
                .filter(row -> row.getIndex() > endIndex)
                .forEach(row -> row.moveIndex(endIndex - startIndex + 1));
        graph.updatePolyline();
        addSavedState();
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
        x = pixelsToInches(x);
        y = pixelsToInches(y);
        if (nextIndex == -1) {
            addNewPointRow(new Point(x, y, intercept), true);
        } else {
            rows.get(nextIndex).setPoint(new Point(x, y, rows.get(nextIndex).getPoint().isIntercept()));
            addSavedState();
            setNextIndex(-1);
        }
        graph.updatePolyline();
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
            int change = shift ? ctrl ? 24 : 1 : ctrl ? 12 : 6; //key = in -> shift=1, none=6, ctrl=12, both=24
            double x = row.getPoint().getX();
            double y = row.getPoint().getY();
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
                    addSavedState();
                    break;
                case ESCAPE:
                    setNextIndex(-1);
                    break;
            }
            row.setPoint(new Point(x, y, row.getPoint().isIntercept()));
            graph.updatePolyline();
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
                    rowAtIndex(Math.max(0, focusedIndex - 1)).getAllNodes().get(focusedColumn).requestFocus();
                    break;
                case DOWN:
                    rowAtIndex(Math.min(rows.size() - 1, focusedIndex + 1)).getAllNodes().get(focusedColumn).requestFocus();
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
    private void mnuExport() {
        String url = Config.getStringProperty("csv_out_dir") + "\\" + Config.getStringProperty("path_name");
        try (CSVWriter<Point> leftWriter = new CSVWriter<>(url + "_left.csv");
             CSVWriter<Point> rightWriter = new CSVWriter<>(url + "_right.csv")) {
            leftWriter.writeObjects("Dist,Vel", path,
                    Point::getLeftPos,
                    Point::getLeftVel);
            rightWriter.writeObjects("Dist,Vel", path,
                    Point::getRightPos,
                    Point::getRightVel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void mnuSavePoints() {
        String url = Config.getStringProperty("points_save_dir") + "\\" + Config.getStringProperty("path_name");
        try (CSVWriter<Point> writer = new CSVWriter<>(url + "_save.csv")) {
            writer.writeObjects("X,Y,Intercept,Velocity,Override,Reverse", controlPoints,
                    Point::getX,
                    Point::getY,
                    Point::isIntercept,
                    Point::getTargetVelocity,
                    Point::isOverrideMaxVel,
                    Point::isReverse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void mnuOpenPoints() {
        FileChooser saveFile = new FileChooser();
        saveFile.setTitle("Choose save");
        try {
            saveFile.setInitialDirectory(new File(Config.getStringProperty("points_save_dir")));
            try (BufferedReader reader = new BufferedReader(new FileReader(saveFile.showOpenDialog(root.getScene().getWindow())))) {
                deleteAllPoints();
                reader.lines()
                        .skip(1)
                        .map(line -> line.split(","))
                        .map(vals ->
                                new Point(Double.parseDouble(vals[0]),
                                        Double.parseDouble(vals[1]),
                                        Boolean.parseBoolean(vals[2]),
                                        Double.parseDouble(vals[3]),
                                        Boolean.parseBoolean(vals[4]),
                                        Boolean.parseBoolean(vals[5])))
                        .forEach(point -> addNewPointRow(point, false));
                addSavedState();
            } catch (IOException e) {
                e.printStackTrace();
            }
            graph.updatePolyline();
        } catch (IllegalArgumentException e) {
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
        graph.updatePolyline();
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
        graph.updatePolyline();
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

    @FXML
    private void mnuChangeSaveOut() {
        configUpdate();
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Save location");
        dirChooser.setInitialDirectory(new File(Config.getStringProperty("points_save_dir", "src")));
        File dir = dirChooser.showDialog(root.getScene().getWindow());
        config.setProperty("points_save_dir", dir.getAbsolutePath());
    }

    @FXML
    private void redraw() {
        graph.updatePolyline();
    }
}
