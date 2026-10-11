package pl.audiofix.ui.theme;

import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import java.util.prefs.Preferences;

/**
 * The user's theme choice, remembered in Preferences, and the theme it means right now.
 * In SYSTEM mode the theme follows Windows also while the app is running.
 */
public final class ThemeManager {

    static final String KEY = "theme";

    private final ObjectProperty<ThemeChoice> choice;
    private final ObjectBinding<Theme> theme;

    /** Choice saved for this user, system color scheme from Windows. Needs a running JavaFX platform. */
    public ThemeManager() {

        this(Preferences.userNodeForPackage(ThemeManager.class), Platform.getPreferences().colorSchemeProperty());
    }

    /** Choice kept in the given node, system color scheme from the given value (tests pass their own). */
    public ThemeManager(Preferences preferences, ObservableValue<ColorScheme> systemScheme) {

        choice = new SimpleObjectProperty<>(ThemeChoice.parse(preferences.get(KEY, null)));
        choice.addListener((observable, old, picked) -> preferences.put(KEY, picked.name()));
        theme = Bindings.createObjectBinding(() -> choice.get().resolve(systemScheme.getValue()), choice, systemScheme);
    }

    public ObjectProperty<ThemeChoice> choiceProperty() {
        return choice;
    }

    public ThemeChoice getChoice() {
        return choice.get();
    }

    public void setChoice(ThemeChoice picked) {
        choice.set(picked);
    }

    /** Theme to show: the choice resolved against the current Windows color scheme. */
    public ObservableValue<Theme> themeProperty() {
        return theme;
    }
}
