package ui;

import bezier.Point;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PointRow { //Make this a node?
    private int index;
    private TextField xText, yText, velText;
    private CheckBox interceptBox, velBox;
    private ComboBox<String> comboBox;
    private Point point;

    public PointRow(int index) {
        this.index = index;
    }

    public Point getPoint() {
        return point;
    }

    public void setPoint(Point p) {
        point = p;
        xText.setText(String.valueOf(p.getX()));
        yText.setText(String.valueOf(p.getY()));
        interceptBox.setSelected(p.isIntercept());
        velText.setText(String.valueOf(p.getTargetVelocity()));
    }

    public int getIndex() {
        return index;
    }

    public void makeAllNodes(Point p) {
        point = p;
        makeXText(p.getXString());
        makeYText(p.getYString());
        makeCheckBox(p.isIntercept());
        makeVelText();
        makeComboBox();
        makeVelBox(p.isOverrideMaxVel());
    }

    private void makeXText(String x) {
        xText = new TextField(x);
        xText.setMaxWidth(100);
        GridPane.setColumnIndex(xText, 0);
        GridPane.setRowIndex(xText, index);
    }

    private void makeYText(String y) {
        yText = new TextField(y);
        yText.setMaxWidth(100);
        GridPane.setColumnIndex(yText, 1);
        GridPane.setRowIndex(yText, index);
    }

    private void makeCheckBox(boolean intercept) {
        interceptBox = new CheckBox();
        interceptBox.setSelected(intercept);
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
        ObservableList results = FXCollections.observableArrayList();
        results.addAll((PointMenuResult[]) PointMenuResult.values());
        comboBox = new ComboBox<>(FXCollections.observableArrayList(results));
        comboBox.setMaxWidth(100);
        GridPane.setColumnIndex(comboBox, 4);
        GridPane.setRowIndex(comboBox, index);
    }

    private void makeVelBox(boolean overrides) {
        velBox = new CheckBox();
        velBox.setSelected(overrides);
        velBox.setMaxWidth(100);
        GridPane.setColumnIndex(velBox, 5);
        GridPane.setRowIndex(velBox, index);
        velBox.setAlignment(Pos.CENTER);
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
        nodes.add(velBox);
        return nodes;
    }

    public ComboBox<String> getComboBox() {
        return comboBox;
    }

    public double getXValue() {
        return Double.parseDouble(xText.getText().trim());
    }

    public double getYValue() {
        return Double.parseDouble(yText.getText().trim());
    }

    public boolean getInterceptValue() {
        return interceptBox.isSelected();
    }

    public boolean getVelCheckValue() {
        return velBox.isSelected();
    }

    public double getVelValue() {
        return Double.parseDouble(velText.getText().trim());
    }

    public void updatePoint() {
        point.setX(getXValue());
        point.setY(getYValue());
        point.setIntercept(getInterceptValue());
        point.setTargetVelocity(getVelValue());
        point.overrideMaxVel(getVelCheckValue());
    }

    public PointRow(PointRow original) {
        makeAllNodes(original.getPoint());
        setIndex(original.getIndex());
    }
}
