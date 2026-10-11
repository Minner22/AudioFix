package pl.audiofix.ui.theme;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    private static final Color DARK_CANVAS = Color.web("#141418");
    private static final Color DARK_FIELD = Color.web("#25252d");
    private static final Color DARK_BORDER = Color.web("#383a48");
    private static final Color DARK_TEXT = Color.web("#f1f5f9");
    private static final Color DARK_CONVERT_BG = Color.web("#2b170c");

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

    @AfterEach
    void backToLight() throws Exception {
        onFxThread(() -> {
            Theme.setCurrent(Theme.LIGHT);
            return null;
        });
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
    void darkValuesComeBeforeTheSameRules() {
        List<String> sheets = Theme.DARK.stylesheets();

        assertEquals(2, sheets.size());
        assertTrue(sheets.get(0).endsWith("/pl/audiofix/ui/theme/dark.css"), sheets.get(0));
        assertEquals(Theme.LIGHT.stylesheets().get(1), sheets.get(1), "one base.css for both themes");
    }

    @Test
    void lightIsCurrentBeforeAnyChoice() {
        assertEquals(Theme.LIGHT, Theme.current());
    }

    @Test
    void applyUsesCurrentTheme() throws Exception {
        List<String> sheets = onFxThread(() -> {
            Theme.setCurrent(Theme.DARK);
            Scene scene = new Scene(new VBox());
            Theme.apply(scene);
            return List.copyOf(scene.getStylesheets());
        });

        assertEquals(Theme.DARK.stylesheets(), sheets);
    }

    // ---------------------------------------------------------------- switching while windows are open

    @Test
    void openSceneFollowsThemeChanges() throws Exception {
        Scene scene = onFxThread(() -> {
            Scene created = new Scene(new VBox());
            Theme.apply(created);
            Theme.setCurrent(Theme.DARK);
            return created;
        });
        assertEquals(Theme.DARK.stylesheets(), onFxThread(() -> List.copyOf(scene.getStylesheets())));

        onFxThread(() -> {
            Theme.setCurrent(Theme.LIGHT);
            return null;
        });
        List<String> sheets = onFxThread(() -> List.copyOf(scene.getStylesheets()));
        assertEquals(Theme.LIGHT.stylesheets(), sheets);
        assertFalse(sheets.stream().anyMatch(sheet -> sheet.endsWith("dark.css")), "dark values must go");
    }

    @Test
    void openAlertFollowsThemeChanges() throws Exception {
        List<String> sheets = onFxThread(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            Theme.apply(alert);
            Theme.setCurrent(Theme.DARK);
            return List.copyOf(alert.getDialogPane().getStylesheets());
        });

        assertEquals(Theme.DARK.stylesheets(), sheets);
    }

    @Test
    void applyingTwiceKeepsOneSetOfStylesheets() throws Exception {
        List<String> sheets = onFxThread(() -> {
            Scene scene = new Scene(new VBox());
            Theme.apply(scene);
            Theme.apply(scene);
            Theme.setCurrent(Theme.DARK);
            return List.copyOf(scene.getStylesheets());
        });

        assertEquals(Theme.DARK.stylesheets(), sheets);
    }

    @Test
    void closedWindowIsNotKeptByTheTheme() throws Exception {
        // every alert gets a listener on the current theme; a strong one would keep each closed alert in memory
        WeakReference<Scene> scene = onFxThread(() -> {
            Scene created = new Scene(new VBox(new Label("Konwersja zakończona")));
            Theme.apply(created);
            return new WeakReference<>(created);
        });

        for (int i = 0; i < 50 && scene.get() != null; i++) {
            System.gc();
            Thread.sleep(20);
        }
        assertNull(scene.get());
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

    @Test
    void selectedRadioDotIsCentered() throws Exception {
        // Modena draws the dot in two layers, the first shifted 1px down; with one flat color that shift stays
        Bounds[] bounds = onFxThread(() -> {
            RadioButton radio = new RadioButton();
            radio.setSelected(true);
            Theme.apply(new Scene(new VBox(radio)));
            radio.applyCss();
            radio.layout();
            Region circle = (Region) radio.lookup(".radio");
            Region dot = (Region) radio.lookup(".dot");
            assertEquals(Insets.EMPTY, dot.getBackground().getFills().getFirst().getInsets());
            return new Bounds[] {circle.getLayoutBounds(), dot.getBoundsInParent()};
        });

        Bounds circle = bounds[0];
        Bounds dot = bounds[1];
        assertEquals(circle.getWidth() / 2, dot.getMinX() + dot.getWidth() / 2, 0.01, "horizontal center");
        assertEquals(circle.getHeight() / 2, dot.getMinY() + dot.getHeight() / 2, 0.01, "vertical center");
    }

    @Test
    void checkMarkIsNotShifted() throws Exception {
        Insets insets = onFxThread(() -> {
            CheckBox check = new CheckBox();
            check.setSelected(true);
            Theme.apply(new Scene(new VBox(check)));
            check.applyCss();
            check.layout();
            return ((Region) check.lookup(".mark")).getBackground().getFills().getFirst().getInsets();
        });

        assertEquals(Insets.EMPTY, insets);
    }

    // ---------------------------------------------------------------- resolved values: dark

    @Test
    void darkWindowBackgroundIsDeepCanvas() throws Exception {
        assertEquals(DARK_CANVAS, fill(styled(Theme.DARK), 0));
    }

    @Test
    void darkButtonIsRaisedFieldWithHairlineBorder() throws Exception {
        Button button = styled(Theme.DARK).getChildren().stream()
                .filter(n -> n instanceof Button b && b.getStyleClass().size() == 1)
                .map(Button.class::cast).findFirst().orElseThrow();

        assertEquals(DARK_BORDER, fill(button, 0));
        assertEquals(DARK_FIELD, fill(button, 1));
        assertEquals(DARK_TEXT, button.getTextFill());
    }

    @Test
    void darkTextIsLight() throws Exception {
        Label label = (Label) styled(Theme.DARK).lookup(".label");

        assertEquals(DARK_TEXT, label.getTextFill());
    }

    @Test
    void darkKeepsAccentForPrimaryButton() throws Exception {
        Button primary = (Button) styled(Theme.DARK).lookup(".button-primary");

        assertEquals(ACCENT, fill(primary, 1));
        assertEquals(Color.WHITE, primary.getTextFill());
    }

    @Test
    void darkThemeUsesBundledFonts() throws Exception {
        onFxThread(ThemeFonts::load);
        VBox root = styled(Theme.DARK);

        assertEquals("Geist", ((Label) root.lookup(".label")).getFont().getFamily());
        assertEquals("JetBrains Mono", ((TextArea) root.lookup(".console")).getFont().getFamily());
    }

    @Test
    void darkTableRowsUseFieldColor() throws Exception {
        assertEquals(DARK_FIELD, fill(darkRow(false), 1), "rows are lighter than the panel around the table");
    }

    @Test
    void darkRowsToConvertStandOut() throws Exception {
        Region row = darkRow(true);

        assertEquals(Color.web("#522714"), fill(row, 0), "orange line under the row");
        assertEquals(Color.web("#fb923c"), fill(row, 1), "orange left edge");
        assertEquals(3, row.getBackground().getFills().get(2).getInsets().getLeft(), "the edge is 3px wide");
        assertEquals(DARK_CONVERT_BG, fill(row, 2), "warm background over the edge layer");
    }

    // ---------------------------------------------------------------- helpers

    private static VBox styled() throws Exception {
        return styled(Theme.LIGHT);
    }

    /** A track table row styled by the dark theme, with or without :needs-conversion. */
    private static Region darkRow(boolean needsConversion) throws Exception {
        return onFxThread(() -> {
            Region row = new Region();
            row.getStyleClass().add("table-row-cell");
            row.pseudoClassStateChanged(PseudoClass.getPseudoClass("needs-conversion"), needsConversion);
            new Scene(new VBox(row)).getStylesheets().setAll(Theme.DARK.stylesheets());
            row.applyCss();
            return row;
        });
    }

    /** A small window with the controls the given theme styles, after CSS has been applied. */
    private static VBox styled(Theme theme) throws Exception {
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
            new Scene(root, 400, 400).getStylesheets().setAll(theme.stylesheets());
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
