package utils;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
    private static Properties config = new Properties();
    private File configFile;
    private Node[] nodes;

    public Config(String path, Node... configNodes) {
//        config = new Properties();
        nodes = configNodes;
        configFile = new File(path);
        try {
            config.load(new FileInputStream(configFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateNodes();
    }

    private static String getProperty(String key) {
        return config.getProperty(key);
    }

    private void setProperty(String key, String value) {
        config.setProperty(key, value);
    }

    public static double getDoubleProperty(String key) {
        return Utils.parseDouble(getProperty(key));
    }

    public static boolean getBooleanProperty(String key) {
        return Utils.parseBoolean(getProperty(key));
    }

    private void updateNodes() {
        for (Node node : nodes) {
            String key = node.getAccessibleRoleDescription();
            if (node instanceof TextField) {
                ((TextField) node).setText(getProperty(key));
            } else if (node instanceof CheckBox) {
                ((CheckBox) node).setSelected(Boolean.valueOf(getProperty(key)));
            }
        }
    }

    public void updateConfig() {
        for (Node node : nodes) {
            String key = node.getAccessibleRoleDescription(), value = null;
            if (node instanceof TextField) {
                value = ((TextField) node).getText();
            } else if (node instanceof CheckBox) {
                value = String.valueOf(((CheckBox) node).isSelected());
            }
            setProperty(key, value != null ? value : " ");
        }
        try {
            config.store(new FileOutputStream(configFile), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
