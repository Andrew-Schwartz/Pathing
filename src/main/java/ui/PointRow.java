package ui;

import bezier.Point;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class PointRow { //Make this a node?
    private int index;
    private TextField xText, yText, velText;
    private CheckBox interceptBox;
    private ComboBox<String> comboBox;
    private Point point;

    public PointRow(int index, Point p) {
        this.index = index;
        makeAllNodes(p);
    }

    public Point getPoint() {
        return point;
    }

    public void setPoint(Point p) {
        Function<Double, String> round2Str = x -> String.valueOf(Math.round(x * 100) / 100.);
        point = p;
        xText.setText(round2Str.apply(p.getX()));
        yText.setText(round2Str.apply(p.getY()));
        interceptBox.setSelected(p.isIntercept());
        velText.setText(String.valueOf(p.getTargetVelocity()));
    }

    public int getIndex() {
        return index;
    }

    private void makeAllNodes(Point p) {
        makeXText();
        makeYText();
        makeCheckBox();
        makeVelText();
        makeComboBox();
        setPoint(p);
    }

    private void makeXText() {
        xText = new TextField();
        xText.setMaxWidth(100);
        GridPane.setColumnIndex(xText, 0);
        GridPane.setRowIndex(xText, index);
    }

    private void makeYText() {
        yText = new TextField();
        yText.setMaxWidth(100);
        GridPane.setColumnIndex(yText, 1);
        GridPane.setRowIndex(yText, index);
    }

    private void makeCheckBox() {
        interceptBox = new CheckBox();
        interceptBox.setSelected(false);
        interceptBox.setMaxWidth(100);
        GridPane.setColumnIndex(interceptBox, 2);
        GridPane.setRowIndex(interceptBox, index);
        interceptBox.setAlignment(Pos.CENTER);
    }

    private void makeVelText() {
        velText = new TextField();
        velText.setMaxWidth(100);
        GridPane.setColumnIndex(velText, 3);
        GridPane.setRowIndex(velText, index);
    }

    private void makeComboBox() {
//        ObservableList<String> results = FXCollections.observableArrayList();
//        results.addAll(Arrays.stream(PointMenuResult.values())
//                             .map(PointMenuResult::toString)
//                             .toArray(String[]::new));
        comboBox = new ComboBox<>(FXCollections.observableArrayList(
                Arrays.stream(PointMenuResult.values())
                        .map(PointMenuResult::toString)
                        .toArray(String[]::new)));
        comboBox.setMaxWidth(100);
        GridPane.setColumnIndex(comboBox, 4);
        GridPane.setRowIndex(comboBox, index);
    }

    public void setIndex(int index) {
        this.index = index;
        getAllNodes().forEach(node -> GridPane.setRowIndex(node, index));
    }

    public void moveIndex(int delta) {
        setIndex(index - delta);
    }

    public List<Node> getAllNodes() {
        List<Node> nodes = new ArrayList<>();
        nodes.add(xText);
        nodes.add(yText);
        nodes.add(interceptBox);
        nodes.add(velText);
        nodes.add(comboBox);
        return nodes;
    }

    public ComboBox<String> getComboBox() {
        return comboBox;
    }

    private double getXValue() {
        return Utils.parseDouble(xText.getText().trim());
    }

    private double getYValue() {
        return Utils.parseDouble(yText.getText().trim());
    }

    private boolean getInterceptValue() {
        return interceptBox.isSelected();
    }

    private double getVelValue() {
        return Utils.parseDouble(velText.getText().trim());
    }

    public void updatePoint() {
        point.setX(getXValue());
        point.setY(getYValue());
        point.setIntercept(getInterceptValue());
        point.setTargetVelocity(getVelValue());
    }

}
