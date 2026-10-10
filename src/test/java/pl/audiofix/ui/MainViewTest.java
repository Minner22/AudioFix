package pl.audiofix.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test of main-view.fxml: the file loads with its controller (every fx:id and #handler
 * matches MainController) and the nodes later tasks rely on exist.
 */
class MainViewTest {

    private static Parent root;
    private static Object controller;

    @BeforeAll
    static void loadView() throws Exception {
        FxTestSupport.startJavaFx();

        // a typo in fx:id, onAction or fx:controller fails here
        FxTestSupport.onFxThread(() -> {
            FXMLLoader loader = FxTestSupport.loadMainView();
            root = loader.getRoot();
            controller = loader.getController();
        });
    }

    @Test
    void controllerIsMainController() {
        assertInstanceOf(MainController.class, controller);
    }

    @Test
    void buttonsExist() {
        for (String id : List.of("addFilesButton", "ffmpegSettingsButton", "aboutButton", "removeFromQueueButton",
                "changeOutputButton", "startButton", "quickConvertButton", "cancelButton",
                "applyToAllButton", "outputFolderButton")) {
            Button button = lookup(id, Button.class);
            assertNotNull(button.getOnAction(), "no onAction handler for " + id);
            assertFalse(button.getText().isBlank(), "no text on " + id);
        }
    }

    @Test
    void queueTrackTableAndLogExist() {
        lookup("queueList", ListView.class);
        lookup("trackTable", TableView.class);
        lookup("outputField", TextField.class);
        lookup("progressBar", ProgressBar.class);
        lookup("logArea", TextArea.class);
    }

    @Test
    void trackTableHasAllColumnsInOrder() {
        TableView<?> table = lookup("trackTable", TableView.class);

        List<String> ids = table.getColumns().stream().map(TableColumn::getId).toList();

        assertEquals(List.of("keepColumn", "indexColumn", "typeColumn", "codecColumn", "channelsColumn",
                "languageColumn", "titleColumn", "actionColumn", "defaultColumn"), ids);
    }

    @Test
    void quickConvertButtonExplainsWhatItDoes() {
        Button quick = lookup("quickConvertButton", Button.class);

        assertNotNull(quick.getTooltip(), "quick conversion should have a tooltip");
        assertTrue(quick.getTooltip().getText().contains("PCM"), quick.getTooltip().getText());
    }

    @Test
    void logIsReadOnly() {
        assertFalse(lookup("logArea", TextArea.class).isEditable());
    }

    @Test
    void cancelIsDisabledUntilConversionStarts() {
        assertTrue(lookup("cancelButton", ButtonBase.class).isDisable());
    }

    @Test
    void progressBarStretchesWithWindow() {
        // ProgressBar does not grow by default - without maxWidth="Infinity" it stays ~100 px wide
        double maxWidth = lookup("progressBar", ProgressBar.class).getMaxWidth();
        assertTrue(maxWidth >= Double.MAX_VALUE, "progressBar maxWidth should be Infinity, was " + maxWidth);
    }

    @Test
    void trackTableShowsHintWhenEmpty() {
        assertNotNull(lookup("trackTable", TableView.class).getPlaceholder());
    }

    @Test
    void fxmlDoesNotAttachStylesheetsItself() {
        // the theme is set on the Scene (Theme.apply), so #35 can switch it for the whole window
        assertEquals(List.of(), root.getStylesheets());
    }

    @Test
    void buttonVariantsFollowDesign() {
        assertTrue(lookup("startButton", Button.class).getStyleClass().contains("button-primary"));
        assertTrue(lookup("quickConvertButton", Button.class).getStyleClass().contains("button-warning"));
        assertTrue(lookup("removeFromQueueButton", Button.class).getStyleClass().contains("button-danger"));
    }

    @Test
    void buttonsAreNeverNarrowerThanTheirText() {
        // long Polish (and later English) labels must not be cut to "Ustawienia…"
        for (Node node : root.lookupAll(".button")) {
            assertEquals(Region.USE_PREF_SIZE, ((Region) node).getMinWidth(),
                    "minWidth=\"-Infinity\" missing on " + node.getId());
        }
    }

    @Test
    void sectionHeadersAndConsoleHaveStyleClasses() {
        long headers = root.lookupAll(".section-header").size();

        assertEquals(2, headers, "Kolejka and Ścieżki should be section headers");
        assertTrue(lookup("logArea", TextArea.class).getStyleClass().contains("console"));
    }

    @Test
    void wholeWindowAcceptsDroppedFiles() {
        // drag and drop handlers sit on the root, so a drop anywhere in the window works
        assertNotNull(root.getOnDragOver());
        assertNotNull(root.getOnDragDropped());
        assertNotNull(root.getOnDragExited(), "the highlight must be removed when the drag leaves the window");
        assertTrue(root.getStyleClass().contains("drop-target"));
    }

    // ---------------------------------------------------------------- helpers

    private static <T extends Node> T lookup(String id, Class<T> type) {
        Node node = root.lookup("#" + id);
        assertNotNull(node, "no node with fx:id=" + id);
        return assertInstanceOf(type, node, "wrong type for " + id);
    }
}
