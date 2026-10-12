package pl.audiofix.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.audiofix.ui.theme.Theme;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.audiofix.ui.FxTestSupport.callOnFxThread;
import static pl.audiofix.ui.FxTestSupport.onFxThread;

/** The colored console on its own: cells, copying, following the end. */
class LogConsoleTest {

    private static final String COMMAND = "C:\\ffmpeg\\bin\\ffmpeg.exe -i in.mkv out.mkv";

    private ListView<String> console;
    private final ObservableList<String> lines = FXCollections.observableArrayList(
            "AudioFix 0.4.0 | ffmpeg 7.0.1", COMMAND, "Stream mapping:", "Error opening output file", "Gotowe: out.mkv");

    @BeforeAll
    static void startJavaFx() {
        FxTestSupport.startJavaFx();
    }

    @BeforeEach
    void createConsole() throws Exception {
        onFxThread(() -> {
            console = new ListView<>();
            console.getStyleClass().add("console");
            LogConsole.configure(console);
            Theme.apply(new Scene(console, 600, 96));
            console.setItems(lines);
            FxTestSupport.layout(console);
        });
    }

    @Test
    void everyLineHasItsKindClass() throws Exception {
        assertEquals(List.of("log-version", "log-command", "log-plain", "log-error", "log-success"),
                callOnFxThread(() -> lines.stream().map(line -> kindClass(cell(line))).toList()));
    }

    @Test
    void commandIsShownWithPrompt() throws Exception {
        assertEquals("> " + COMMAND, callOnFxThread(() -> cell(COMMAND).getText()));
    }

    @Test
    void selectedLinesAreCopiedInLogOrder() throws Exception {
        String text = callOnFxThread(() -> {
            console.getSelectionModel().select(3);
            console.getSelectionModel().select(1);   // picked bottom-up, copied top-down
            return LogConsole.selectedText(console);
        });

        assertEquals(COMMAND + System.lineSeparator() + "Error opening output file", text);
    }

    @Test
    void copiedCommandHasNoPrompt() throws Exception {
        // "> " is only how the console shows it; pasted into a terminal the command must run as is
        String text = callOnFxThread(() -> {
            console.getSelectionModel().select(1);
            return LogConsole.selectedText(console);
        });

        assertEquals(COMMAND, text);
    }

    @Test
    void nothingSelectedCopiesNothing() throws Exception {
        assertEquals("", callOnFxThread(() -> LogConsole.selectedText(console)));
    }

    @Test
    void newLinesScrollToTheEnd() throws Exception {
        onFxThread(() -> {
            IntStream.range(0, 50).forEach(i -> lines.add("linia " + i));
            FxTestSupport.layout(console);
        });

        assertTrue(callOnFxThread(() -> visible("linia 49")), "last line should be visible");
    }

    @Test
    void anotherFilesLogIsShownFromItsEnd() throws Exception {
        ObservableList<String> other = FXCollections.observableArrayList();
        IntStream.range(0, 50).forEach(i -> other.add("inny " + i));

        onFxThread(() -> {
            console.setItems(other);
            FxTestSupport.layout(console);
        });

        assertTrue(callOnFxThread(() -> visible("inny 49")));
    }

    @Test
    void previousLogNoLongerScrolls() throws Exception {
        ObservableList<String> other = FXCollections.observableArrayList();
        IntStream.range(0, 50).forEach(i -> other.add("inny " + i));
        onFxThread(() -> {
            console.setItems(other);
            console.scrollTo(0);   // the user reads the start of the shown log
            FxTestSupport.layout(console);
            lines.add("stary");    // while the previously shown file keeps logging
            FxTestSupport.layout(console);
        });

        assertTrue(callOnFxThread(() -> visible("inny 0")), "the shown log must stay where the user left it");
    }

    @Test
    void linesHaveThemeColors() throws Exception {
        // light theme values; a typo in a base.css selector would leave every line in the plain color
        assertEquals(Color.web("#4ec9b0"), callOnFxThread(() -> cell(COMMAND).getTextFill()));
        assertEquals(Color.web("#f87171"), callOnFxThread(() -> cell("Error opening output file").getTextFill()));
        assertEquals(Color.web("#34d399"), callOnFxThread(() -> cell("Gotowe: out.mkv").getTextFill()));
        assertEquals(Color.web("#cccccc"), callOnFxThread(() -> cell("Stream mapping:").getTextFill()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reusedCellKeepsOnlyOneColor() throws Exception {
        // while scrolling a cell moves to another line; the old kind class must go
        List<String> kinds = callOnFxThread(() -> {
            ListCell<String> cell = (ListCell<String>) console.getCellFactory().call(console);
            cell.updateListView(console);
            cell.updateIndex(1);   // command
            cell.updateIndex(3);   // error
            return cell.getStyleClass().stream().filter(c -> c.startsWith("log-")).toList();
        });

        assertEquals(List.of("log-error"), kinds);
    }

    @Test
    void emptyConsoleShowsNoPlaceholderText() throws Exception {
        assertInstanceOf(Region.class, callOnFxThread(() -> console.getPlaceholder()));
    }

    // ---------------------------------------------------------------- helpers

    private ListCell<?> cell(String line) {
        return console.lookupAll(".list-cell").stream()
                .map(node -> (ListCell<?>) node)
                .filter(cell -> line.equals(cell.getItem()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no cell for " + line));
    }

    private boolean visible(String line) {
        return console.lookupAll(".list-cell").stream()
                .map(node -> (ListCell<?>) node)
                .anyMatch(cell -> line.equals(cell.getItem()) && cell.isVisible()
                        && cell.localToScene(cell.getLayoutBounds()).getMaxY() <= console.localToScene(console.getLayoutBounds()).getMaxY() + 0.5
                        && cell.localToScene(cell.getLayoutBounds()).getMinY() >= console.localToScene(console.getLayoutBounds()).getMinY() - 0.5);
    }

    private static String kindClass(ListCell<?> cell) {
        List<String> kinds = cell.getStyleClass().stream().filter(c -> c.startsWith("log-")).toList();
        assertEquals(1, kinds.size(), "exactly one kind class: " + kinds);
        return kinds.getFirst();
    }
}
