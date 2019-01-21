package utils;

import bezier.Feet;
import bezier.FeetPerSecond;
import bezier.FeetPerSecondSquared;
import bezier.Inches;
import bezier.Seconds;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
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
        nodes = configNodes;
        configFile = new File("./src/main/resources/" + path);
        try {
            config.load(new FileInputStream(configFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateNodes();
    }

    public void setProperty(String key, String value) {
        config.setProperty(key, value);
        store();
    }

    public static double getDoubleProperty(String key) {
        return Double.parseDouble(config.getProperty(key, "0"));
    }

    public static boolean getBooleanProperty(String key) {
        return Boolean.parseBoolean(config.getProperty(key, "false"));
    }

    public static String getStringProperty(String key) {
        return config.getProperty(key);
    }

    public static String getStringProperty(String key, String defaultVal) {
        return config.getProperty(key, defaultVal);
    }

    private void updateNodes() {
        for (Node node : nodes) {
            String key = node.getAccessibleRoleDescription();
            if (node instanceof TextField) {
                ((TextField) node).setText(config.getProperty(key));
            } else if (node instanceof CheckBox) {
                ((CheckBox) node).setSelected(Boolean.valueOf(config.getProperty(key)));
            } else if (node instanceof ComboBox) {
                ((ComboBox) node).setValue(config.getProperty(key));
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
            } else if (node instanceof ComboBox) {
                value = ((ComboBox) node).getValue().toString();
            }
            setProperty(key, value != null ? value : "");
        }
    }

    private void store() {
        try {
            config.store(new FileOutputStream(configFile), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //short methods for getting doubles
    public static Inches wheelRadius() {
        return new Inches(getDoubleProperty("wheel_radius"));
    }

    public static Inches width() {
        return new Inches(getDoubleProperty("width"));
    }

    public static Inches length() {
        return new Inches(getDoubleProperty("length"));
    }

    public static FeetPerSecondSquared maxAccel() {
        return new FeetPerSecondSquared(new Feet(getDoubleProperty("max_accel")));
    }

    public static FeetPerSecond maxVel() {
        return new FeetPerSecond(new Feet(getDoubleProperty("max_vel")));
    }

    public static Seconds timeStep() {
        return new Seconds(getDoubleProperty("time_step"));
    }

    public static double ticksPerRev() {
        return getDoubleProperty("ticks_per_rev");
    }

}
