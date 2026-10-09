package pl.audiofix.ui.theme;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Theme stylesheets attached to windows and dialogs, and the -af-* variables actually resolved by JavaFX.
 */
class ThemeTest {

    private static final Color CANVAS = Color.web("#f8fafc");
    private static final Color SURFACE = Color.web("#ffffff");
    private static final Color BORDER = Color.web("#cbd5e1");
    private static final Color TEXT = Color.web("#0f172a");
    private static final Color ACCENT = Color.web("#0284c7");
    private static final Color CONVERT = Color.web("#ea580c");
    private static final Color DANGER = Color.web("#b91c1c");

    @BeforeAll
    static void startJavaFx() {
        CompletableFuture<Void> started = new CompletableFuture<>();
        try {
            Platform.startup(() -> started.complete(null));
        } catch (IllegalStateException alreadyStarted) {
            started.complete(null);
        }
        Platform.setImplicitExit(false);
        started.orTimeout(10, TimeUnit.SECONDS).join();
    }

    // ---------------------------------------------------------------- stylesheets

    @Test
    void themeValuesComeBeforeRules() {
        List<String> sheets = Theme.LIGHT.stylesheets();

        assertEquals(2, sheets.size());
        assertTrue(sheets.get(0).endsWith("/pl/audiofix/ui/theme/light.css"), sheets.get(0));
        assertTrue(sheets.get(1).endsWith("/pl/audiofix/ui/theme/base.css"), sheets.get(1));
    }

    @Test
    void lightIsCurrentUntilDarkModeExists() {
        assertEquals(Theme.LIGHT, Theme.current());
    }

    @Test
    void applyReplacesSceneStylesheets() throws Exception {
        List<String> sheets = onFxThread(() -> {
            Scene scene = new Scene(new VBox());
            scene.getStylesheets().add("old.css");
            Theme.apply(scene);
            return List.copyOf(scene.getStylesheets());
        });

        assertEquals(Theme.current().stylesheets(), sheets);
    }

    @Test
    void applyStylesAlertDialogs() throws Exception {
        // alerts have their own window, so the main scene's stylesheets do not reach them
        List<String> sheets = onFxThread(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            Theme.apply(alert);
            return List.copyOf(alert.getDialogPane().getStylesheets());
        });

        assertEquals(Theme.current().stylesheets(), sheets);
    }

    // ---------------------------------------------------------------- resolved values

    @Test
    void windowBackgroundIsCanvas() throws Exception {
        VBox root = styled();

        assertEquals(CANVAS, fill(root, 0));
    }

    @Test
    void buttonIsFlatWithHairlineBorder() throws Exception {
        Button button = styled().getChildren().stream()
                .filter(n -> n instanceof Button b && b.getStyleClass().size() == 1)
                .map(Button.class::cast).findFirst().orElseThrow();

        assertEquals(BORDER, fill(button, 0), "1px border layer");
        assertEquals(SURFACE, fill(button, 1), "flat surface, no gradient");
        assertEquals(TEXT, button.getTextFill());
        assertEquals(null, button.getEffect(), "no shadow");
    }

    @Test
    void buttonVariantsUseAccentColors() throws Exception {
        VBox root = styled();

        Button primary = (Button) root.lookup(".button-primary");
        assertEquals(ACCENT, fill(primary, 1));
        assertEquals(Color.WHITE, primary.getTextFill());
        assertEquals(CONVERT, ((Button) root.lookup(".button-warning")).getTextFill());
        assertEquals(DANGER, ((Button) root.lookup(".button-danger")).getTextFill());
    }

    @Test
    void modenaControlsPickUpAccent() throws Exception {
        // the progress bar is not restyled color by color - it gets the accent through -fx-accent / .bar
        VBox root = styled();

        Region bar = (Region) root.lookup(".progress-bar > .bar");
        assertNotNull(bar);
        assertEquals(ACCENT, fill(bar, 0));
    }

    @Test
    void lightThemeUsesSegoeUiAndConsolas() throws Exception {
        VBox root = styled();

        assertEquals("Segoe UI", ((Label) root.lookup(".label")).getFont().getFamily());
        assertEquals("Consolas", ((TextArea) root.lookup(".console")).getFont().getFamily());
    }

    @Test
    void sectionHeaderIsBoldAndBigger() throws Exception {
        VBox root = styled();

        Label header = (Label) root.lookup(".section-header");
        assertEquals(13, header.getFont().getSize(), 0.01);
        assertTrue(header.getFont().getStyle().contains("Bold"), header.getFont().toString());
    }

    // ---------------------------------------------------------------- helpers

    /** A small window with the controls the theme styles, after CSS has been applied. */
    private static VBox styled() throws Exception {
        return onFxThread(() -> {
            Label header = new Label("Ścieżki");
            header.getStyleClass().add("section-header");
            Button primary = button("Start", "button-primary");
            Button warning = button("Szybka konwersja", "button-warning");
            Button danger = button("Usuń z kolejki", "button-danger");
            TextArea console = new TextArea("ffmpeg");
            console.getStyleClass().add("console");
            ProgressBar progress = new ProgressBar(0.5);

            VBox root = new VBox(new Label("Plik wyjściowy:"), header, new Button("Zmień…"),
                    primary, warning, danger, console, progress);
            Theme.apply(new Scene(root, 400, 400));
            root.applyCss();
            root.layout();
            return root;
        });
    }

    private static Button button(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        return button;
    }

    private static Paint fill(Node node, int layer) {
        Region region = (Region) node;
        assertNotNull(region.getBackground(), "no background on " + node);
        return region.getBackground().getFills().get(layer).getFill();
    }

    private interface FxSupplier<T> {
        T get() throws Exception;
    }

    private static <T> T onFxThread(FxSupplier<T> action) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        CompletableFuture<Void> done = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.set(action.get());
                done.complete(null);
            } catch (Throwable t) {
                done.completeExceptionally(t);
            }
        });
        try {
            done.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw e;
        }
        return result.get();
    }
}
