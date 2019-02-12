package ui;

import bezier.GraphicalBezier;
import bezier.Point;
import bezier.units.Inches;
import bezier.units.Pixels;
import bezier.units.Seconds;
import bezier.units.derived.LinearVelocity;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
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
import javafx.scene.layout.AnchorPane;
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
import java.util.Objects;

import static java.util.stream.Collectors.toCollection;
import static utils.Utils.aboutEquals;

public class UIController {
    @FXML
    private AnchorPane paneImg;

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
            cfgTicksPerInch,
            cfgLength,
            cfgMaxVel,
            cfgMaxAccel,
            cfgJerk,
            cfgTimeStep,
            cfgPathName;
    @FXML
    private ComboBox<String> cfgDrawWheelType;

    private static Image backgroundImage = new Image("images/2019BWFromFeildDrawing.JPG");
    private ArrayList<ArrayList<PointRow>> previousStates; //for undo/redo
    private ArrayList<PointRow> rows;
    private int nextIndex,
            gridDnDIndex = -1, //-1 means nothing is being dragged
            dragStartIndex,
            currentState;
    private PointRow draggedRow;
    private static Config config;
    private GraphingUtil graph;
    private boolean pointDrag = false;
    private boolean isClicking;

    @FXML
    private void initialize() {
        cfgDrawWheelType.setItems(FXCollections.observableArrayList(
                Arrays.stream(WheelPathType.values())
                        .map(WheelPathType::toString)
                        .toArray(String[]::new)
        ));
        config = new Config(cfgDrawWheelType, cfgLength, cfgMaxAccel, cfgMaxVel, cfgJerk,
                cfgRadius, cfgWidth, cfgTimeStep, cfgPathName, cfgTicksPerInch);

//        backgroundImage = new Image(Config.getStringProperty("img_path", "src\\images\\FRC2018.png"));

//        double width = backgroundImage.getWidth(), height = backgroundImage.getHeight();
//        final double maxHeight = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight() * .5; //Percent of screen to take up, is an estimate
//        final double proportion = maxHeight / height;
//        width *= proportion;
//        height *= proportion;

//        imgField.setFitWidth(width);
//        imgField.setFitHeight(height);

//        root.applyCss();
//        root.layout();
//        root.getHeight();

        imgField.setImage(backgroundImage);

        rows = new ArrayList<>();
        previousStates = new ArrayList<>();
        setNextIndex(-1);
        grdPoints.setOnDragOver(event -> {
            if (gridDnDIndex == -1) return;
            double y = event.getY(),
                    rowHeight = grdPoints.getVgap() + rows.get(0).getComboBox().getHeight();
            gridDnDIndex = (int) Math.floor(y / rowHeight);
            dndHandling(draggedRow, false);
            graph.updateAndGraph(pointDrag);
        });
        tabVel.setOnSelectionChanged(event -> graph.graphMotion());
        graph = new GraphingUtil(new ArrayList<>(), new ArrayList<>(), rows,
                polyPos, polyLeft, polyRight,
                chtLeft, chtRight, chtCenter,
                tabVel, paneImg);

        Task openPointsTask = new Task<Void>() {
            @Override
            protected Void call() {
                boolean ran = false;
                while (!ran) {
                    if (isCancelled()) break;
                    if (root == null) continue;
                    if (root.getChildren().stream().anyMatch(Objects::isNull)) continue;
                    ran = true;

                    File file = new File(Config.getStringProperty("points_save_dir") + "/" + Config.getStringProperty("path_name") + "_save.csv");

                    Objects.requireNonNull(pointsFromFile(file)).forEach(p -> addNewPointRow(p, false));
                    graph.updateAndGraph(pointDrag);
                }

                return null;
            }
        };

        new Thread(openPointsTask).start();
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
                graph.updateAndGraph(pointDrag);
            });
        if (node instanceof CheckBox)
            ((CheckBox) node).setOnAction(event -> {
                row.updatePoint();
                addSavedState();
                graph.updateAndGraph(pointDrag);
            });
        if (node instanceof ComboBox)
            ((ComboBox) node).setOnAction(event -> {
                handleComboResults(row.getComboBox().getValue(), row.getIndex());
                ((ComboBox) node).getSelectionModel().clearSelection();
                root.requestFocus(); //to prevent double selection
            });

        //Drag and Drop listeners
        node.setOnDragDetected(event -> {
            gridDnDIndex = row.getIndex();
            draggedRow = row;
            dragStartIndex = row.getIndex();
            Dragboard db = node.startDragAndDrop(TransferMode.MOVE);
            var content = new ClipboardContent();
            content.putString("this has to exist or nothing works! :( "); //<---- self documenting code
            db.setContent(content);
        });
        node.setOnDragDone(event -> {
            dndHandling(row, draggedRow.getIndex() != dragStartIndex);
            graph.updateAndGraph(pointDrag);
        });
    }

    private void handleComboResults(String res, int index) {
        if (nextIndex != -1)
            return;
        PointMenuResult result = Arrays.stream(PointMenuResult.values())
                .filter(pmr -> pmr.toString().equals(res))
                .findFirst()
                .orElse(PointMenuResult.NONE);

        switch (result) {
            case MENU:
                var menu = PopupFactory.menu(rowAtIndex(index).getPoint());
                menu.showAndWait().ifPresent(rowAtIndex(index)::setPoint);
                addSavedState();
                graph.updateAndGraph(pointDrag);
                break;
            case DELETE_POINT:
                deletePoints(index, index);
                break;
            case POINT_MOVE_MODE:
                setNextIndex(index);
                pointHighlight.setCenterX(rowAtIndex(index).getPoint().getX().pixels().getValue());
                pointHighlight.setCenterY(imageHeight().minus(rowAtIndex(index).getPoint().getY().pixels()).getValue());
                break;
            case TOGGLE_OVERRIDE_VEL:
                rowAtIndex(index).getPoint().setOverrideMaxVel(!rowAtIndex(index).getPoint().isOverrideMaxVel());
                addSavedState();
                graph.updateAndGraph(pointDrag);
                break;
            case TOGGLE_BACKWARDS:
                rowAtIndex(index).getPoint().setReverse(!rowAtIndex(index).getPoint().isReverse());
                addSavedState();
                graph.updateAndGraph(pointDrag);
                break;
            case MAX_VEL:
                //TODO implement. Should use physics to determine max vel reachable on way up AND down
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
        graph.updateAndGraph(pointDrag);
        addSavedState();
    }

    private PointRow rowAtIndex(int index) {
        return rows.parallelStream()
                .filter(row -> row.getIndex() == index)
                .findFirst()
                .orElseThrow();
    }

    @FXML
    private void releaseEvent(MouseEvent mouseEvent) {
        isClicking = false;
        if (pointDrag) {
            dragPoint(mouseEvent);
        } else {
            addPoint(mouseEvent);
        }
    }

    private void dragPoint(MouseEvent mouseEvent) {
        Pixels x = new Pixels(mouseEvent.getX());
        Pixels y = new Pixels(mouseEvent.getY());
        if (x.getValue() < 0 || y.getValue() < 0 || x.getValue() > imageWidth().getValue() || y.getValue() > imageHeight().getValue())
            return;
        y = imageHeight().minus(y);
//
//        System.out.println("x = " + x);
//        System.out.println("y = " + y);
    }

    private void addPoint(MouseEvent mouseEvent) {
        Pixels x = new Pixels(mouseEvent.getX());
        Pixels y = new Pixels(mouseEvent.getY());
        boolean intercept = mouseEvent.getButton() == MouseButton.PRIMARY && !mouseEvent.isControlDown();
        if (x.getValue() < 0 || y.getValue() < 0 || x.getValue() > imageWidth().getValue() || y.getValue() > imageHeight().getValue())
            return;
        y = imageHeight().minus(y.getValue());
        if (nextIndex == -1) {
            addNewPointRow(new Point(x.inches(), y.inches(), intercept), true);
        } else {
            rowAtIndex(nextIndex).setPoint(new Point(x.inches(), y.inches(), rowAtIndex(nextIndex).getPoint().isIntercept()));
            addSavedState();
            setNextIndex(-1);
        }
        graph.updateAndGraph(pointDrag);
    }

    @FXML
    private void mouseMoveEvent(MouseEvent event) {
        //highlight only appears if circles are visible
        cursorHighlight.setCenterX(Math.max(0, Math.min(imageWidth().getValue(), event.getX())));
        cursorHighlight.setCenterY(Math.max(0, Math.min(imageHeight().getValue(), event.getY())));

        //drag point if in pointDragMode
        if (pointDrag) { //todo detect if it is clicked
            graph.getRows().stream()
                    .map(PointRow::getPoint)
                    .filter(p -> aboutEquals(p.getX().pixels().getValue(), event.getX(), 9))
                    .filter(p -> aboutEquals(imageHeight().minus(p.getY().pixels()).getValue(), event.getY(), 9))
                    .findFirst()
                    .ifPresent(p -> {
                        p.setX(new Pixels(event.getX()).inches());
                        p.setY(imageHeight().minus(new Pixels(event.getY())).inches());
                    });
            graph.updateAndGraph(true);
        }
    }

    @FXML
    private void keyReleasedEvent(KeyEvent keyEvent) {
        if (nextIndex != -1) {
            PointRow row = rowAtIndex(nextIndex);
            root.requestFocus();
            boolean ctrl = keyEvent.isControlDown(),
                    shift = keyEvent.isShiftDown();
            int change = shift ? ctrl ? 20 : 1 : ctrl ? 10 : 5; //key = in -> shift=1, none=6, ctrl=12, both=24
            Inches x = row.getPoint().getX();
            Inches y = row.getPoint().getY();
            switch (keyEvent.getCode()) {
                case UP:
                    y = new Inches(y.getValue() + change);
                    break;
                case DOWN:
                    y = new Inches(y.getValue() - change);
                    break;
                case LEFT:
                    x = new Inches(x.getValue() - change);
                    break;
                case RIGHT:
                    x = new Inches(x.getValue() + change);
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
            pointHighlight.setCenterX(x.pixels().getValue());
            pointHighlight.setCenterY(imageHeight().minus(y.pixels()).getValue());
            graph.updateAndGraph(pointDrag);
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
                    rowAtIndex(Math.max(0, focusedIndex - 1))
                            .getAllNodes()
                            .get(focusedColumn)
                            .requestFocus();
                    break;
                case DOWN:
                    rowAtIndex(Math.min(rows.size() - 1, focusedIndex + 1))
                            .getAllNodes()
                            .get(focusedColumn)
                            .requestFocus();
                    break;
            }
        }
    }

    private void setNextIndex(int nextIndex) {
        pointHighlight.setVisible(nextIndex != -1);
        cursorHighlight.setVisible(nextIndex != -1);
        this.nextIndex = nextIndex;
    }

    @FXML
    private void mnuOpenImage() {
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
        try {
            backgroundImage = new Image(chosenImage.toURI().toURL().toString());
        } catch (MalformedURLException e) {
            //fail silently, just means they clicked cancel or X on the popup
        }
        imgField.setImage(backgroundImage);
        imgField.setFitWidth(imageWidth().getValue());
        imgField.setFitHeight(imageHeight().getValue());
    }

    @FXML
    private void mnuExport() {
        export(graph.getPath(), Config.getStringProperty("path_name"));
    }

    private void export(ArrayList<Point> pathPoints, String pathName) {
        String url = Config.getStringProperty("csv_out_dir") + "\\" + pathName;
        try (var leftWriter = new CSVWriter<Point>(url + "_left.csv");
             var rightWriter = new CSVWriter<Point>(url + "_right.csv")) {
            double firstHeading = pathPoints.get(0).getHeading().getValue();
            leftWriter.writeObjects("Dist,Vel,Heading", pathPoints,
                    p -> -p.getLeftPos().ticks().getValue(),
                    p -> -p.getLeftVel().ticksPerHundredMillis().getValue(),
                    p -> p.getHeading().getValue() - firstHeading);
            rightWriter.writeObjects("Dist,Vel,Heading", pathPoints,
                    p -> p.getRightPos().ticks().getValue(),
                    p -> p.getRightVel().ticksPerHundredMillis().getValue(),
                    p -> p.getHeading().getValue() - firstHeading);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void mnuSavePoints() {
        savePoints(graph.getControlPoints());
        String url = Config.getStringProperty("points_save_dir") + "\\" + Config.getStringProperty("path_name");
    }

    private void savePoints(ArrayList<Point> controlPoints) {
        String url = Config.getStringProperty("points_save_dir") + "\\" + Config.getStringProperty("path_name");
        try (var writer = new CSVWriter<Point>(url + "_save.csv")) {
            writer.writeObjects("X,Y,Intercept,Velocity,Override,Reverse", controlPoints,
                    p -> p.getX().getValue(),
                    p -> p.getY().getValue(),
                    Point::isIntercept,
                    p -> p.getTargetVelocity().getValue(),
                    Point::isOverrideMaxVel,
                    Point::isReverse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * recalculates all paths in current save directory, in case config values have changed or something
     */
    @FXML
    private void updateAllPaths() {
        File[] files = new File(Config.getStringProperty("points_save_dir")).listFiles(pathname -> pathname.getAbsolutePath().endsWith("_save.csv"));
        for (File file : Objects.requireNonNull(files)) {
            var points = pointsFromFile(file);
            if (points != null) {
                export(GraphicalBezier.generateSpline(points), pathNameFromFile(file));
            }
        }
    }

    @FXML
    private void mnuOpenPoints() {
        FileChooser saveFile = new FileChooser();
        saveFile.setTitle("Choose save");

        saveFile.setInitialDirectory(new File(Config.getStringProperty("points_save_dir")));
        File file = saveFile.showOpenDialog(root.getScene().getWindow());
        if (file != null) {
            deleteAllPoints();
            var newPoints = pointsFromFile(file);
            if (newPoints != null) {
                graph.getControlPoints().addAll(newPoints);
                addSavedState();
                graph.updateAndGraph(pointDrag);
                cfgPathName.setText(pathNameFromFile(file));
                config.updateConfig();
            }
        }
    }


    private ArrayList<Point> pointsFromFile(File file) {
        try (var reader = new BufferedReader(new FileReader(file))) {
            return reader.lines()
                    .skip(1)
                    .map(line -> line.split(","))
                    .map(vals -> new Point(
                                    Double.parseDouble(vals[0]),
                                    Double.parseDouble(vals[1]),
                                    Boolean.parseBoolean(vals[2]),
                                    new LinearVelocity<>(new Inches(Double.parseDouble(vals[3])), new Seconds(1.0)),
                                    Boolean.parseBoolean(vals[4]),
                                    Boolean.parseBoolean(vals[5])
                            )
                    )
                    .collect(toCollection(ArrayList::new));
        } catch (IOException e) {
            //fail silently, just means the selected file doesn't exist
        }
        return null;
    }

    public String pathNameFromFile(File file) {
        return file.getName().substring(0, file.getName().length() - "_save.csv".length());
    }

    public static Pixels imageHeight() {
        return new Pixels(backgroundImage.getHeight());
    }

    public static Pixels imageWidth() {
        return new Pixels(backgroundImage.getWidth());
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
        graph.updateAndGraph(pointDrag);
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
        graph.updateAndGraph(pointDrag);
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
        graph.updateAndGraph(pointDrag);
    }

    @FXML
    private void mnuPointDrag() {
        pointDrag = !pointDrag;
        graph.updateAndGraph(pointDrag);
        if (!pointDrag) {
            graph.clearCircles();
        }
    }

    @FXML
    private void mnuSaveAll() {
        mnuSavePoints();
        mnuExport();
    }
}
