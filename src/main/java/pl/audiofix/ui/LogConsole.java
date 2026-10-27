package pl.audiofix.ui;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Region;

import java.util.Arrays;
import java.util.List;

/**
 * The log console under the buttons: one line per log entry, colored by its kind.
 * A ListView instead of a TextArea, because a TextArea has one color for all its text.
 * Lines are selected whole and copied with Ctrl+C or the context menu.
 */
final class LogConsole {

    static final KeyCombination COPY = new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);

    private static final List<String> KIND_CLASSES = Arrays.stream(LogLines.Kind.values())
            .map(LogLines.Kind::styleClass)
            .toList();

    private LogConsole() {
    }

    static void configure(ListView<String> console) {

        console.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        console.setPlaceholder(new Region());   // no "No content in List" while there is no file
        console.setCellFactory(list -> new LogCell());

        console.setOnKeyPressed(event -> {
            if (COPY.match(event)) {
                copy(selectedText(console));
                event.consume();
            }
        });

        MenuItem copySelected = new MenuItem(Messages.get("console.copy"));
        copySelected.setOnAction(event -> copy(selectedText(console)));
        MenuItem copyAll = new MenuItem(Messages.get("console.copyAll"));
        copyAll.setOnAction(event -> copy(String.join(System.lineSeparator(), console.getItems())));
        console.setContextMenu(new ContextMenu(copySelected, copyAll));

        // follow the end: new lines of the running file, and the end of a file's log when it is shown
        ListChangeListener<String> toEnd = change -> scrollToEnd(console);
        console.itemsProperty().addListener((observable, previous, lines) -> {
            if (previous != null) {
                previous.removeListener(toEnd);
            }
            if (lines != null) {
                lines.addListener(toEnd);
            }
            scrollToEnd(console);
        });
    }

    /** The context menu in the current language (#31). */
    static void translate(ListView<String> console) {

        List<MenuItem> items = console.getContextMenu().getItems();
        items.get(0).setText(Messages.get("console.copy"));
        items.get(1).setText(Messages.get("console.copyAll"));
    }

    /** Selected lines in the order they are in the log (selected indices are always ascending), one per line. */
    static String selectedText(ListView<String> console) {

        ObservableList<String> lines = console.getItems();
        return String.join(System.lineSeparator(), console.getSelectionModel().getSelectedIndices().stream()
                .map(lines::get)
                .toList());
    }

    private static void copy(String text) {

        if (text.isEmpty()) {
            return;
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private static void scrollToEnd(ListView<String> console) {

        if (!console.getItems().isEmpty()) {
            console.scrollTo(console.getItems().size() - 1);
        }
    }

    private static final class LogCell extends ListCell<String> {

        @Override
        protected void updateItem(String line, boolean empty) {

            super.updateItem(line, empty);
            getStyleClass().removeAll(KIND_CLASSES);   // cells move between lines while scrolling
            if (empty || line == null) {
                setText(null);
                return;
            }

            LogLines.Kind kind = LogLines.kind(line);
            setText(kind == LogLines.Kind.COMMAND ? "> " + line : line);
            getStyleClass().add(kind.styleClass());
        }
    }
}
