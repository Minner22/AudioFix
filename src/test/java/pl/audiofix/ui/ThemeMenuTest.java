package pl.audiofix.ui;

import javafx.application.ColorScheme;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.audiofix.ui.theme.Theme;
import pl.audiofix.ui.theme.ThemeChoice;
import pl.audiofix.ui.theme.ThemeManager;

import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.audiofix.ui.FxTestSupport.callOnFxThread;
import static pl.audiofix.ui.FxTestSupport.onFxThread;

/** The "Motyw" menu in a loaded main window, wired like AudioFixApp does it (#35). */
class ThemeMenuTest {

    private Preferences prefs;
    private final ObjectProperty<ColorScheme> windows = new SimpleObjectProperty<>(ColorScheme.LIGHT);

    private Parent root;
    private ThemeManager themes;

    @BeforeAll
    static void startJavaFx() {
        FxTestSupport.startJavaFx();
    }

    @BeforeEach
    void setUp() {
        prefs = Preferences.userRoot().node("audiofix-test/" + UUID.randomUUID());
    }

    @AfterEach
    void tearDown() throws Exception {
        onFxThread(() -> {
            Theme.currentProperty().unbind();
            Theme.setCurrent(Theme.LIGHT);
        });
        prefs.removeNode();
    }

    @Test
    void savedChoiceIsCheckedInMenu() throws Exception {
        prefs.put("theme", "DARK");
        load();

        assertEquals(List.of(false, false, true), checked());
    }

    @Test
    void systemIsCheckedOnFirstStart() throws Exception {
        load();

        assertEquals(List.of(true, false, false), checked());
    }

    @Test
    void pickingDarkRestylesWindowAtOnce() throws Exception {
        load();

        pick("darkThemeItem");
        assertEquals(Theme.DARK.stylesheets(), stylesheets());
        assertEquals(Color.web("#141418"), callOnFxThread(() -> ((Region) root).getBackground().getFills().getFirst().getFill()),
                "new values are applied, not only the file list");

        pick("lightThemeItem");
        assertEquals(Theme.LIGHT.stylesheets(), stylesheets());
    }

    @Test
    void pickedThemeIsRemembered() throws Exception {
        load();

        pick("darkThemeItem");

        assertEquals("DARK", prefs.get("theme", null));
        assertEquals(ThemeChoice.DARK, themes.getChoice());
    }

    @Test
    void systemChoiceFollowsWindowsWhileRunning() throws Exception {
        load();

        onFxThread(() -> windows.set(ColorScheme.DARK));
        assertEquals(Theme.DARK.stylesheets(), stylesheets());

        onFxThread(() -> windows.set(ColorScheme.LIGHT));
        assertEquals(Theme.LIGHT.stylesheets(), stylesheets());
    }

    @Test
    void forcedLightIgnoresDarkWindows() throws Exception {
        load();
        pick("lightThemeItem");

        onFxThread(() -> windows.set(ColorScheme.DARK));

        assertEquals(Theme.LIGHT.stylesheets(), stylesheets());
    }

    @Test
    void themeMenuIsAsHighAsOtherToolbarButtons() throws Exception {
        load();

        double[] heights = callOnFxThread(() -> new double[] {
                ((MenuButton) root.lookup("#themeButton")).getHeight(),
                ((Button) root.lookup("#aboutButton")).getHeight()});
        assertTrue(heights[1] > 0);
        assertEquals(heights[1], heights[0], 0.5);
    }

    // ---------------------------------------------------------------- helpers

    private void load() throws Exception {
        onFxThread(() -> {
            themes = new ThemeManager(prefs, windows);
            Theme.currentProperty().bind(themes.themeProperty());
            FXMLLoader loader = FxTestSupport.loadMainView();
            root = loader.getRoot();
            ((MainController) loader.getController()).setThemeManager(themes);
        });
    }

    private void pick(String id) throws Exception {
        onFxThread(() -> {
            MenuItem item = items().stream().filter(candidate -> id.equals(candidate.getId())).findFirst().orElseThrow();
            item.fire();
            FxTestSupport.layout(root);
        });
    }

    private List<MenuItem> items() {
        return ((MenuButton) root.lookup("#themeButton")).getItems();
    }

    private List<Boolean> checked() throws Exception {
        return callOnFxThread(() -> items().stream().map(item -> ((RadioMenuItem) item).isSelected()).toList());
    }

    private List<String> stylesheets() throws Exception {
        return callOnFxThread(() -> List.copyOf(root.getScene().getStylesheets()));
    }
}
