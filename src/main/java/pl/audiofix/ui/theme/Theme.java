package pl.audiofix.ui.theme;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

import java.net.URL;
import java.util.List;
import java.util.Map;

public enum Theme {

    LIGHT("light.css"),
    DARK("dark.css");

    static final String BASE = "base.css";

    private static final ObjectProperty<Theme> CURRENT = new SimpleObjectProperty<>(LIGHT);
    private static final String LISTENER_KEY = Theme.class.getName();

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

    /** Theme of every window and dialog; changing it restyles the open ones at once. */
    public static ObjectProperty<Theme> currentProperty() {
        return CURRENT;
    }

    public static Theme current() {
        return CURRENT.get();
    }

    public static void setCurrent(Theme theme) {
        CURRENT.set(theme);
    }

    /** Styles the scene with the current theme and keeps it following theme changes. */
    public static void apply(Scene scene) {
        follow(scene.getStylesheets(), scene.getProperties());
    }

    /** Same for a dialog: it has its own window, so the main scene's stylesheets do not reach it. */
    public static void apply(Dialog<?> dialog) {

        DialogPane pane = dialog.getDialogPane();
        follow(pane.getStylesheets(), pane.getProperties());
    }

    private static void follow(ObservableList<String> stylesheets, Map<Object, Object> owner) {

        stylesheets.setAll(current().stylesheets());

        // the scene keeps its listener (a second apply replaces it), the weak wrapper lets a closed window go
        ChangeListener<Theme> listener = (observable, old, theme) -> stylesheets.setAll(theme.stylesheets());
        owner.put(LISTENER_KEY, listener);
        CURRENT.addListener(new WeakChangeListener<>(listener));
    }

    private static String url(String name) {

        URL url = Theme.class.getResource(name);
        if (url == null) {
            throw new IllegalStateException("Missing stylesheet " + name);
        }

        return url.toExternalForm();
    }
}
