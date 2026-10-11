package pl.audiofix.ui.theme;

import javafx.application.ColorScheme;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Remembered choice and the theme it gives, with a fake Windows color scheme and an isolated Preferences node. */
class ThemeManagerTest {

    private Preferences prefs;
    private final ObjectProperty<ColorScheme> windows = new SimpleObjectProperty<>(ColorScheme.LIGHT);

    @BeforeEach
    void setUp() {
        prefs = Preferences.userRoot().node("audiofix-test/" + UUID.randomUUID());
    }

    @AfterEach
    void tearDown() throws BackingStoreException {
        prefs.removeNode();
    }

    // ---------------------------------------------------------------- remembered choice

    @Test
    void systemIsTheDefault() {
        assertEquals(ThemeChoice.SYSTEM, manager().getChoice());
    }

    @Test
    void startingDoesNotWriteAnything() {
        manager();

        assertNull(prefs.get(ThemeManager.KEY, null), "nothing to save until the user picks a theme");
    }

    @Test
    void savedChoiceIsRestored() {
        prefs.put(ThemeManager.KEY, "DARK");

        assertEquals(ThemeChoice.DARK, manager().getChoice());
    }

    @Test
    void unknownSavedChoiceMeansSystem() {
        prefs.put(ThemeManager.KEY, "SEPIA");

        assertEquals(ThemeChoice.SYSTEM, manager().getChoice());
    }

    @Test
    void pickedChoiceIsSaved() {
        manager().setChoice(ThemeChoice.LIGHT);

        assertEquals("LIGHT", prefs.get(ThemeManager.KEY, null));
    }

    @Test
    void pickedChoiceSurvivesRestart() {
        manager().setChoice(ThemeChoice.DARK);

        assertEquals(ThemeChoice.DARK, manager().getChoice(), "a new manager reads what the previous one saved");
    }

    // ---------------------------------------------------------------- resolved theme

    @Test
    void systemChoiceStartsWithWindowsScheme() {
        windows.set(ColorScheme.DARK);

        assertEquals(Theme.DARK, manager().themeProperty().getValue());
    }

    @Test
    void systemChoiceFollowsWindowsWhileRunning() {
        ThemeManager manager = manager();
        List<Theme> seen = record(manager);

        windows.set(ColorScheme.DARK);
        windows.set(ColorScheme.LIGHT);

        assertEquals(List.of(Theme.DARK, Theme.LIGHT), seen);
    }

    @Test
    void forcedThemeIgnoresWindows() {
        ThemeManager manager = manager();
        manager.setChoice(ThemeChoice.LIGHT);

        windows.set(ColorScheme.DARK);

        assertEquals(Theme.LIGHT, manager.themeProperty().getValue());
    }

    @Test
    void pickingThemeChangesItAtOnce() {
        ThemeManager manager = manager();
        List<Theme> seen = record(manager);

        manager.setChoice(ThemeChoice.DARK);
        manager.setChoice(ThemeChoice.SYSTEM);

        assertEquals(List.of(Theme.DARK, Theme.LIGHT), seen, "back to SYSTEM shows the Windows scheme again");
    }

    // ---------------------------------------------------------------- helpers

    private ThemeManager manager() {
        return new ThemeManager(prefs, windows);
    }

    /** Every theme change the manager reports from now on. */
    private static List<Theme> record(ThemeManager manager) {
        List<Theme> seen = new ArrayList<>();
        manager.themeProperty().addListener((observable, old, theme) -> seen.add(theme));
        return seen;
    }
}
