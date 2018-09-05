package ui;

import bezier.Point;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.List;

public class PointRow { //Make this a node?
    private int index;
    private TextField xText, yText;
    private CheckBox checkBox;
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
        checkBox.setSelected(p.isIntercept());
    }

    public int getIndex() {
        return index;
    }

    public void makeAllNodes(Point p) {
        point = p;
        makeXText(p.getXString());
        makeYText(p.getYString());
        makeCheckBox(p.isIntercept());
        makeComboBox();
    }

    public void makeXText(String x) {
        xText = new TextField(x);
        xText.setMaxWidth(100);
        GridPane.setColumnIndex(xText, 0);
        GridPane.setRowIndex(xText, index);
    }

    public void makeYText(String y) {
        yText = new TextField(y);
        yText.setMaxWidth(100);
        GridPane.setColumnIndex(yText, 1);
        GridPane.setRowIndex(yText, index);
    }

    public void makeCheckBox(boolean intercept) {
        checkBox = new CheckBox();
        checkBox.setSelected(intercept);
        checkBox.setMaxWidth(100);
        GridPane.setColumnIndex(checkBox, 2);
        GridPane.setRowIndex(checkBox, index);
        checkBox.setAlignment(Pos.CENTER);
    }

    public void makeComboBox() {
        comboBox = new ComboBox<>(FXCollections.observableArrayList(
                PointMenuResult.DELETE_POINT.toString(),
                PointMenuResult.POINT_EDIT_MODE.toString(),
                PointMenuResult.REORDER_POINT.toString()
        ));
        comboBox.setMaxWidth(100);
        GridPane.setColumnIndex(comboBox, 3);
        GridPane.setRowIndex(comboBox, index);
    }

    public void setIndex(int index) {
        this.index = index;
        GridPane.setRowIndex(xText, index);
        GridPane.setRowIndex(yText, index);
        GridPane.setRowIndex(checkBox, index);
        GridPane.setRowIndex(comboBox, index);
    }

    public void moveIndex(int delta) {
        setIndex(index - delta);
    }

    public List<Node> getAllNodes() {
        List<Node> nodes = new ArrayList<>();
        nodes.add(xText);
        nodes.add(yText);
        nodes.add(checkBox);
        nodes.add(comboBox);
        return nodes;
    }

    public TextField getxText() {
        return xText;
    }

    public TextField getyText() {
        return yText;
    }

    public CheckBox getCheckBox() {
        return checkBox;
    }

    public ComboBox<String> getComboBox() {
        return comboBox;
    }

    public double getXValue() {
        return Double.valueOf(xText.getText());
    }

    public double getYValue() {
        return Double.valueOf(yText.getText());
    }

    public boolean getCheckValue() {
        return checkBox.isSelected();
    }

    public void updatePoint() {
        point.setX(getXValue());
        point.setY(getYValue());
        point.setIntercept(getCheckValue());
    }

    public PointRow(PointRow original) {
        makeAllNodes(original.getPoint());
        setIndex(original.getIndex());
    }
}
