package pl.audiofix.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
        lookup("logList", ListView.class);
    }

    @Test
    void trackTableColumnsHaveFixedWidthsExceptCodecAndTitle() {
        TableView<?> table = lookup("trackTable", TableView.class);

        assertEquals(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS, table.getColumnResizePolicy(), "no horizontal scrolling");
        for (TableColumn<?, ?> column : table.getColumns()) {
            boolean flexible = List.of("codecColumn", "titleColumn").contains(column.getId());
            assertEquals(!flexible, column.getMinWidth() == column.getMaxWidth(), column.getId());
            assertTrue(column.getMinWidth() > 0, "no minimum width on " + column.getId());
            // min = max already fixes the width; the constrained policy leaves non-resizable columns out of its sums
            assertTrue(column.isResizable(), column.getId() + " must stay resizable for the resize policy");
        }
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
        // ffmpeg takes the default audio track first, not the one with the most channels (#69)
        assertTrue(quick.getTooltip().getText().contains("domyślna"), quick.getTooltip().getText());
    }

    @Test
    void consoleLinesCanBeSelectedTogether() {
        // a ListView, not a TextArea: one color per line; whole lines are selected and copied
        ListView<?> console = lookup("logList", ListView.class);

        assertEquals(SelectionMode.MULTIPLE, console.getSelectionModel().getSelectionMode());
        assertNotNull(console.getContextMenu(), "no copy menu");
        assertEquals(List.of("Kopiuj", "Kopiuj wszystko"),
                console.getContextMenu().getItems().stream().map(MenuItem::getText).toList());
    }

    @Test
    void consoleShowsAboutSixLines() {
        ListView<?> console = lookup("logList", ListView.class);

        assertEquals(96, console.getPrefHeight());
        assertEquals(96, console.getMinHeight(), "the console must not shrink in a small window");
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

        assertEquals(2, headers, "queue and tracks titles should be section headers");
        assertTrue(lookup("logList", ListView.class).getStyleClass().contains("console"));
    }

    @Test
    void wholeWindowAcceptsDroppedFiles() {
        // drag and drop handlers sit on the root, so a drop anywhere in the window works
        assertNotNull(root.getOnDragOver());
        assertNotNull(root.getOnDragDropped());
        assertNotNull(root.getOnDragExited(), "the highlight must be removed when the drag leaves the window");
        assertTrue(root.getStyleClass().contains("drop-target"));
    }

    // ---------------------------------------------------------------- layout (#44)

    @Test
    void toolbarHasFileActionsLeftAndThemeAndAboutRight() {
        HBox toolBar = lookup("toolBar", HBox.class);
        List<Node> items = toolBar.getChildren();

        assertEquals(6, items.size(), items.toString());
        assertEquals("addFilesButton", items.get(0).getId());
        assertEquals("ffmpegSettingsButton", items.get(1).getId());
        assertEquals(Priority.ALWAYS, HBox.getHgrow(items.get(2)), "spacer pushes the rest to the right");
        assertTrue(items.get(3).getStyleClass().contains("language-switch"), "PL | EN before the theme (#31)");
        assertEquals(List.of("polishButton", "englishButton"),
                ((HBox) items.get(3)).getChildren().stream().map(Node::getId).toList());
        assertEquals("themeButton", items.get(4).getId());
        assertEquals("aboutButton", items.get(5).getId());
    }

    // ---------------------------------------------------------------- theme menu (#35)

    @Test
    void themeMenuOffersSystemLightAndDark() {
        MenuButton menu = lookup("themeButton", MenuButton.class);
        List<MenuItem> items = menu.getItems();

        assertEquals("Motyw", menu.getText());
        assertEquals(List.of("systemThemeItem", "lightThemeItem", "darkThemeItem"), items.stream().map(MenuItem::getId).toList());
        assertEquals(List.of("Systemowy", "Jasny", "Ciemny"), items.stream().map(MenuItem::getText).toList());
    }

    @Test
    void themeChoicesExcludeEachOther() {
        List<MenuItem> items = lookup("themeButton", MenuButton.class).getItems();

        RadioMenuItem first = assertInstanceOf(RadioMenuItem.class, items.getFirst());
        assertNotNull(first.getToggleGroup(), "radio items without a group can all be checked at once");
        for (MenuItem item : items) {
            assertEquals(first.getToggleGroup(), assertInstanceOf(RadioMenuItem.class, item).getToggleGroup(), item.getId());
        }
    }

    @Test
    void themeMenuHasIconAndFullText() {
        MenuButton menu = lookup("themeButton", MenuButton.class);

        Icon icon = assertInstanceOf(Icon.class, menu.getGraphic(), "no icon on the theme menu");
        assertEquals(Icon.Glyph.THEME, icon.getGlyph());
        assertEquals(Region.USE_PREF_SIZE, menu.getMinWidth(), "minWidth=\"-Infinity\" missing");
    }

    @Test
    void bottomBarHasActionsBeforeProgress() {
        Node start = lookup("startButton", Button.class);
        List<String> ids = start.getParent().getChildrenUnmodifiable().stream().map(Node::getId).toList();

        assertEquals(4, ids.size(), ids.toString());
        assertEquals(List.of("startButton", "quickConvertButton", "cancelButton"), ids.subList(0, 3));
    }

    @Test
    void progressHasStatusAndPercentAboveBar() {
        Node bar = lookup("progressBar", ProgressBar.class);
        VBox info = assertInstanceOf(VBox.class, bar.getParent(), "bar and texts sit in one column");
        HBox texts = assertInstanceOf(HBox.class, info.getChildren().getFirst());

        assertEquals(bar, info.getChildren().getLast());
        assertEquals(Priority.ALWAYS, HBox.getHgrow(info), "the progress column takes the rest of the row");
        assertEquals("progressStatusLabel", texts.getChildren().getFirst().getId());
        assertEquals("progressPercentLabel", texts.getChildren().getLast().getId());
        assertEquals(Region.USE_PREF_SIZE, lookup("progressPercentLabel", Label.class).getMinWidth(),
                "the percent and time left are never cut");
    }

    @Test
    void buttonsHaveIcons() {
        Map<String, Icon.Glyph> icons = Map.of(
                "addFilesButton", Icon.Glyph.ADD,
                "ffmpegSettingsButton", Icon.Glyph.SETTINGS,
                "aboutButton", Icon.Glyph.ABOUT,
                "removeFromQueueButton", Icon.Glyph.REMOVE,
                "startButton", Icon.Glyph.START,
                "quickConvertButton", Icon.Glyph.QUICK);

        icons.forEach((id, glyph) -> {
            Icon icon = assertInstanceOf(Icon.class, lookup(id, Button.class).getGraphic(), "no icon on " + id);
            assertEquals(glyph, icon.getGlyph(), id);
        });
    }

    @Test
    void queueAndTracksAreCardsWithHeaders() {
        for (String id : List.of("queuePanel", "tracksPanel")) {
            VBox panel = lookup(id, VBox.class);
            assertTrue(panel.getStyleClass().contains("panel"), id);
            assertTrue(panel.getChildren().getFirst().getStyleClass().contains("panel-header"), id + " header");
            assertTrue(panel.getChildren().getLast().getStyleClass().contains("panel-footer"), id + " footer");
        }
    }

    @Test
    void queuePanelKeepsItsWidthWhenWindowGrows() {
        VBox queuePanel = lookup("queuePanel", VBox.class);

        assertFalse(SplitPane.isResizableWithParent(queuePanel), "the tracks panel should take the extra width");
        assertEquals(220, queuePanel.getMinWidth());
    }

    @Test
    void outputFieldIsJoinedWithChangeButton() {
        TextField field = lookup("outputField", TextField.class);
        Button change = lookup("changeOutputButton", Button.class);

        assertTrue(field.getStyleClass().containsAll(List.of("path-field", "joined-left")), field.getStyleClass().toString());
        assertTrue(change.getStyleClass().contains("joined-right"));
        assertEquals(field.getParent(), change.getParent(), "field and button in one box");
        assertEquals(0, ((HBox) field.getParent()).getSpacing(), "no gap between them");
    }

    @Test
    void longHeaderTextsAreNeverCut() {
        for (String id : List.of("queueTitleLabel", "queueRemainingLabel", "tracksTitleLabel", "tracksInfoLabel",
                "trackCountLabel")) {
            assertEquals(Region.USE_PREF_SIZE, lookup(id, Label.class).getMinWidth(), "minWidth=\"-Infinity\" missing on " + id);
        }
        assertEquals(OverrunStyle.CENTER_ELLIPSIS, lookup("tracksFileLabel", Label.class).getTextOverrun(),
                "only the file name is shortened, keeping its end");
    }

    // ---------------------------------------------------------------- helpers

    private static <T extends Node> T lookup(String id, Class<T> type) {
        Node node = root.lookup("#" + id);
        assertNotNull(node, "no node with fx:id=" + id);
        return assertInstanceOf(type, node, "wrong type for " + id);
    }
}
