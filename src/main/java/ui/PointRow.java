package ui;

import bezier.Point;
import bezier.units.Feet;
import bezier.units.Inches;
import bezier.units.Seconds;
import bezier.units.derived.LinearVelocity;
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

public class PointRow { //Make this a node?
    private int index;
    private TextField xText, yText, velText;
    private CheckBox interceptBox;
    private ComboBox<String> comboBox;
    private Point point;

    public PointRow(int index, Point p) {
        this.index = index;

        point = p;

        makeAllNodes(p);
    }

    public Point getPoint() {
        return point;
    }

    public void setPoint(Point p) {
        point = p;
        updateDisplay();
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
        comboBox = new ComboBox<>(FXCollections.observableArrayList(
                Arrays.stream(PointMenuResult.values())
                        .map(PointMenuResult::toString)
                        .filter(s -> !s.equals(PointMenuResult.NONE.toString()))
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
        point.setX(new Inches(getXValue()));
        point.setY(new Inches(getYValue()));
        point.setIntercept(getInterceptValue());
        point.setTargetVelocity(new LinearVelocity<>(new Feet(getVelValue()), new Seconds(1.0)).inchesPerSecond());
    }

    public void updateDisplay() {
        xText.setText(String.valueOf(point.getX().round(2).getValue()));
        yText.setText(String.valueOf(point.getY().round(2).getValue()));
        interceptBox.setSelected(point.isIntercept());
        velText.setText(String.valueOf(point.getTargetVelocity().feetPerSecond().round(2).getValue()));
    }
}
