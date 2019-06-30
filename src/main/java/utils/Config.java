package utils;

import bezier.OLDunits.Feet;
import bezier.OLDunits.Inches;
import bezier.OLDunits.Seconds;
import bezier.OLDunits.derived.Acceleration;
import bezier.OLDunits.derived.LinearVelocity;
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

    public Config(Node... configNodes) {
        nodes = configNodes;
        configFile = new File("./src/main/resources/saves/config.properties");
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

    //short methods for getting "doubles"
    public static Inches wheelRadius() {
        return new Inches(getDoubleProperty("wheel_radius"));
    }

    public static Inches width() {
        return new Inches(getDoubleProperty("width"));
    }

    public static Inches length() {
        return new Inches(getDoubleProperty("length"));
    }

    public static Acceleration<Feet, Seconds> maxAccel() {
        return new Acceleration<>(new Feet(getDoubleProperty("max_accel")), new Seconds(1.0));
    }

    public static LinearVelocity<Feet, Seconds> maxVel() {
        return new LinearVelocity<>(new Feet(getDoubleProperty("max_vel")), new Seconds(1.0));
    }

    public static Seconds timeStep() {
        return new Seconds(getDoubleProperty("time_step"));
    }

    public static double ticksPerInch() {
        return getDoubleProperty("ticks_per_inch");
    }

}
