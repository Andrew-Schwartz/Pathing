package ui;

import bezier.Point;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.util.Objects;

import static utils.Utils.parseDouble;

public class MenuFactory {
    private MenuFactory() {
    }

    private static Point point;

    public static Dialog<Point> menu(Point p) {
        point = p.clone();
        var menu = new Dialog<Point>();
        menu.setTitle("Point Menu");

        var confirmType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        menu.getDialogPane().getButtonTypes().addAll(confirmType, ButtonType.CANCEL);

        try {
            GridPane gridPane = FXMLLoader.load(Objects.requireNonNull(MenuFactory.class.getClassLoader().getResource("ui/MenuPopup.fxml")));
            menu.getDialogPane().setContent(gridPane);
            ((TextField) findByID(gridPane, "txtX")).setText(String.valueOf(point.getX()));
            ((TextField) findByID(gridPane, "txtY")).setText(String.valueOf(point.getY()));
            ((CheckBox) findByID(gridPane, "chkIntercept")).setSelected(point.isIntercept());
            ((TextField) findByID(gridPane, "txtVel")).setText(String.valueOf(point.getTargetVelocity()));
            ((CheckBox) findByID(gridPane, "chkOverrideMaxVel")).setSelected(point.isOverrideMaxVel());
            ((CheckBox) findByID(gridPane, "chkReverse")).setSelected(point.isReverse());

            menu.setResultConverter(dialogButton -> {
                if (dialogButton == confirmType) {
                    return new Point(
                            parseDouble(((TextField) findByID(gridPane, "txtX")).getText().trim()),
                            parseDouble(((TextField) findByID(gridPane, "txtY")).getText().trim()),
                            ((CheckBox) findByID(gridPane, "chkIntercept")).isSelected(),
                            parseDouble(((TextField) findByID(gridPane, "txtVel")).getText().trim()),
                            ((CheckBox) findByID(gridPane, "chkOverrideMaxVel")).isSelected(),
                            ((CheckBox) findByID(gridPane, "chkReverse")).isSelected());
                }
                return point;
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return menu;
    }

    private static Node findByID(GridPane grid, String id) {
        return grid.getChildren().stream()
                .filter(node -> !(node instanceof Label))
                .filter(node -> node.getId().equals(id))
                .findFirst()
                .orElseThrow();
//        for (Node child : grid.getChildren()) {
//            if (child instanceof Label)
//                continue;
//            if (child.getId().equals(id))
//                return child;
//        }
//        return null;
    }
}
