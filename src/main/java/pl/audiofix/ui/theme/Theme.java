package pl.audiofix.ui.theme;

import javafx.scene.Scene;
import javafx.scene.control.Dialog;

import java.net.URL;
import java.util.List;

public enum Theme {

    LIGHT("light.css");

    static final String BASE = "base.css";

    private final String fileName;

    Theme(String fileName) {

        this.fileName = fileName;
    }

    public List<String> stylesheets() {
        return List.of(url(fileName), url(BASE));
    }

    String fileName() {
        return fileName;
    }

    public static Theme current() {
        return LIGHT;
    }

    public static void apply(Scene scene) {
        scene.getStylesheets().setAll(current().stylesheets());
    }

    public static void apply(Dialog<?> dialog) {
        dialog.getDialogPane().getStylesheets().setAll(current().stylesheets());
    }

    private static String url(String name) {

        URL url = Theme.class.getResource(name);
        if (url == null) {
            throw new IllegalStateException("Missing stylesheet " + name);
        }

        return url.toExternalForm();
    }
}
