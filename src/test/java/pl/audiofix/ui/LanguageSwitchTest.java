package pl.audiofix.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.audiofix.model.MediaInfo;
import pl.audiofix.model.StreamInfo;
import pl.audiofix.model.StreamType;
import pl.audiofix.model.TrackPlan;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.audiofix.ui.FxTestSupport.callOnFxThread;
import static pl.audiofix.ui.FxTestSupport.onFxThread;

/** PL | EN in the toolbar switches the whole window at once, without a restart, and is remembered (#31). */
class LanguageSwitchTest {

    private Preferences prefs;
    private LanguageSettings languages;
    private Parent root;
    private Map<String, Object> view;

    @BeforeAll
    static void startJavaFx() {
        FxTestSupport.startJavaFx();
    }

    @BeforeEach
    void loadPolishView() throws Exception {
        Messages.use(Locale.of("pl"));
        prefs = Preferences.userRoot().node("audiofix-test/" + UUID.randomUUID());
        languages = new LanguageSettings(prefs, Locale.of("pl", "PL"));
        onFxThread(() -> {
            FXMLLoader loader = FxTestSupport.loadMainView();
            root = loader.getRoot();
            view = loader.getNamespace();
            ((MainController) loader.getController()).setLanguageSettings(languages, view);
        });
    }

    @AfterEach
    void cleanUp() throws BackingStoreException {
        Messages.use(Locale.getDefault());
        prefs.removeNode();
    }

    // ---------------------------------------------------------------- the switch

    @Test
    void switchShowsPlAndEnWithTheCurrentLanguageChosen() throws Exception {
        assertEquals("PL", callOnFxThread(() -> button("polishButton").getText()));
        assertEquals("EN", callOnFxThread(() -> button("englishButton").getText()));
        assertEquals("Polski", callOnFxThread(() -> button("polishButton").getTooltip().getText()));
        assertEquals("English", callOnFxThread(() -> button("englishButton").getTooltip().getText()));
        assertTrue(callOnFxThread(() -> button("polishButton").isSelected()));
        assertFalse(callOnFxThread(() -> button("englishButton").isSelected()));
        assertTrue(callOnFxThread(() -> button("polishButton").getToggleGroup() == button("englishButton").getToggleGroup()),
                "one group: choosing one language unchooses the other");
    }

    @Test
    void savedEnglishIsChosenOnStart() throws Exception {
        Messages.use(Locale.ENGLISH);
        LanguageSettings english = new LanguageSettings(prefs, Locale.of("pl", "PL"));
        english.setLanguage(Language.ENGLISH);

        onFxThread(() -> {
            FXMLLoader loader = FxTestSupport.loadMainView();
            root = loader.getRoot();
            ((MainController) loader.getController()).setLanguageSettings(english, loader.getNamespace());
        });

        assertTrue(callOnFxThread(() -> button("englishButton").isSelected()));
        assertFalse(callOnFxThread(() -> button("polishButton").isSelected()));
    }

    @Test
    void choosingEnglishIsSaved() throws Exception {
        onFxThread(() -> button("englishButton").fire());

        assertEquals(Language.ENGLISH, languages.getLanguage());
        assertEquals("ENGLISH", prefs.get(LanguageSettings.KEY, null));
    }

    @Test
    void clickingTheChosenLanguageAgainKeepsItChosen() throws Exception {
        onFxThread(() -> button("polishButton").fire());

        assertTrue(callOnFxThread(() -> button("polishButton").isSelected()), "a language must always be chosen");
        assertEquals(Language.POLISH, languages.getLanguage());
        assertEquals("Dodaj pliki…", callOnFxThread(() -> ((Button) root.lookup("#addFilesButton")).getText()));
    }

    // ---------------------------------------------------------------- texts from FXML

    @Test
    void fxmlTextsSwitchToEnglishAndBack() throws Exception {
        onFxThread(() -> button("englishButton").fire());

        assertEquals("Add files…", callOnFxThread(() -> ((Button) root.lookup("#addFilesButton")).getText()));
        assertEquals("Theme", callOnFxThread(() -> ((MenuButton) root.lookup("#themeButton")).getText()));
        assertEquals("Dark", callOnFxThread(() -> ((MenuItem) view.get("darkThemeItem")).getText()));
        assertEquals("About", callOnFxThread(() -> ((Button) root.lookup("#aboutButton")).getText()));
        assertEquals("Type", callOnFxThread(() -> ((TableColumn<?, ?>) view.get("typeColumn")).getText()));
        assertEquals("Action", callOnFxThread(() -> ((TableColumn<?, ?>) view.get("actionColumn")).getText()));
        assertEquals("Output file:", callOnFxThread(() -> ((Label) view.get("outputLabel")).getText()));
        assertEquals("Copies “Keep”, the action and the default tracks to the other waiting files with the same track layout.",
                callOnFxThread(() -> ((Button) root.lookup("#applyToAllButton")).getTooltip().getText()));
        assertEquals("PL", callOnFxThread(() -> button("polishButton").getText()), "language names stay in their own language");

        onFxThread(() -> button("polishButton").fire());

        assertEquals("Dodaj pliki…", callOnFxThread(() -> ((Button) root.lookup("#addFilesButton")).getText()));
        assertEquals("Ciemny", callOnFxThread(() -> ((MenuItem) view.get("darkThemeItem")).getText()));
        assertEquals("Typ", callOnFxThread(() -> ((TableColumn<?, ?>) view.get("typeColumn")).getText()));
        assertEquals("Plik wyjściowy:", callOnFxThread(() -> ((Label) view.get("outputLabel")).getText()));
    }

    @Test
    void switchingKeepsTheSameNodesOnScreen() throws Exception {
        Object addFiles = callOnFxThread(() -> root.lookup("#addFilesButton"));

        onFxThread(() -> button("englishButton").fire());

        assertTrue(callOnFxThread(() -> root.lookup("#addFilesButton")) == addFiles, "texts are swapped, the window is not built again");
    }

    // ---------------------------------------------------------------- texts from code

    @Test
    void emptyWindowTextsSwitchToEnglish() throws Exception {
        onFxThread(() -> button("englishButton").fire());

        assertEquals("File queue (0)", text("queueTitleLabel"));
        assertEquals("Remaining: 0", text("queueRemainingLabel"));
        assertEquals("Tracks", text("tracksTitleLabel"));
        assertEquals("Drop videos here or use [Add files…]",
                callOnFxThread(() -> ((Label) ((ListView<?>) root.lookup("#queueList")).getPlaceholder()).getText()));
        assertEquals("Add a file to see its tracks",
                callOnFxThread(() -> ((Label) ((TableView<?>) root.lookup("#trackTable")).getPlaceholder()).getText()));
        assertEquals(List.of("Copy", "Copy all"), callOnFxThread(() -> consoleMenu()));
    }

    @Test
    void loadedFileTextsSwitchToEnglishAndBack() throws Exception {
        add();

        switchTo("englishButton");

        assertEquals("File queue (1)", text("queueTitleLabel"));
        assertEquals("Remaining: 1", text("queueRemainingLabel"));
        assertEquals("Tracks:", text("tracksTitleLabel"));
        assertEquals("4 tracks", text("trackCountLabel"));
        assertEquals(List.of("Video", "Audio", "Audio", "Subtitles"), callOnFxThread(() -> cellTexts("typeColumn")));
        assertEquals("Copy", callOnFxThread(() -> fixedAction(0)));
        assertTrue(callOnFxThread(() -> queueCellTexts()).stream().anyMatch(cell -> cell.contains("Waiting")),
                callOnFxThread(() -> queueCellTexts()).toString());

        switchTo("polishButton");

        assertEquals("Kolejka plików (1)", text("queueTitleLabel"));
        assertEquals("4 ścieżki", text("trackCountLabel"));
        assertEquals(List.of("Wideo", "Audio", "Audio", "Napisy"), callOnFxThread(() -> cellTexts("typeColumn")));
        assertEquals(List.of("Kopiuj", "Kopiuj wszystko"), callOnFxThread(() -> consoleMenu()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void actionButtonOfAnAudioTrackShowsTheNewLanguage() throws Exception {
        add();
        onFxThread(() -> ((TableView<TrackPlan>) root.lookup("#trackTable")).getItems().get(2)
                .setTargetCodec(pl.audiofix.model.AudioCodec.COPY));
        onFxThread(() -> FxTestSupport.layout(root));
        assertEquals("Kopiuj", callOnFxThread(() -> actionButtonText(2)));

        switchTo("englishButton");

        assertEquals("Copy", callOnFxThread(() -> actionButtonText(2)), "the shown value must be rendered again, not only the list");
    }

    @Test
    void queueAndPlansStayAsTheyWere() throws Exception {
        add();
        @SuppressWarnings("unchecked")
        ListView<Object> queue = callOnFxThread(() -> (ListView<Object>) root.lookup("#queueList"));
        Object job = callOnFxThread(() -> queue.getItems().getFirst());

        switchTo("englishButton");

        assertEquals(1, callOnFxThread(() -> queue.getItems().size()));
        assertTrue(callOnFxThread(() -> queue.getItems().getFirst()) == job);
        assertTrue(callOnFxThread(() -> queue.getSelectionModel().getSelectedItem()) == job, "the chosen file stays chosen");
    }

    // ---------------------------------------------------------------- helpers

    private ToggleButton button(String id) {
        return (ToggleButton) root.lookup("#" + id);
    }

    private void switchTo(String id) throws Exception {
        onFxThread(() -> {
            button(id).fire();
            FxTestSupport.layout(root);
        });
    }

    private void add() throws Exception {
        MediaInfo drive = new MediaInfo(Path.of("D:\\Filmy\\Drive.mkv"), 6000, List.of(
                new StreamInfo(0, StreamType.VIDEO, "hevc", null, 0, null, "und", null, true),
                new StreamInfo(1, StreamType.AUDIO, "truehd", null, 8, "7.1", "eng", null, true),
                new StreamInfo(2, StreamType.AUDIO, "ac3", null, 6, "5.1", "eng", null, false),
                new StreamInfo(3, StreamType.SUBTITLE, "subrip", null, 0, null, "pol", null, false)));
        onFxThread(() -> {
            ((MainController) view.get("controller")).addMedia(drive);
            FxTestSupport.layout(root);
        });
    }

    private String text(String id) throws Exception {
        return callOnFxThread(() -> ((Label) root.lookup("#" + id)).getText());
    }

    @SuppressWarnings("unchecked")
    private List<String> consoleMenu() {
        return ((ListView<String>) root.lookup("#logList")).getContextMenu().getItems().stream().map(MenuItem::getText).toList();
    }

    private List<String> queueCellTexts() {
        return root.lookup("#queueList").lookupAll(".label").stream()
                .map(node -> ((Label) node).getText())
                .filter(text -> text != null)
                .toList();
    }

    private List<String> cellTexts(String columnId) {
        return cells(columnId).stream()
                .filter(cell -> !cell.isEmpty())
                .sorted((a, b) -> Integer.compare(a.getIndex(), b.getIndex()))
                .map(TableCell::getText)
                .toList();
    }

    private String fixedAction(int row) {
        return cells("actionColumn").stream()
                .filter(cell -> cell.getIndex() == row)
                .map(cell -> (String) ((ComboBox<?>) cell.getGraphic()).getValue())
                .findFirst()
                .orElseThrow();
    }

    private String actionButtonText(int row) {
        ComboBox<?> codecs = cells("actionColumn").stream()
                .filter(cell -> cell.getIndex() == row)
                .map(cell -> (ComboBox<?>) cell.getGraphic())
                .findFirst()
                .orElseThrow();
        Node shown = ((ComboBoxListViewSkin<?>) codecs.getSkin()).getDisplayNode();   // the button, not a cell of the popup list
        return ((ListCell<?>) shown).getText();
    }

    private List<TableCell<?, ?>> cells(String columnId) {
        return root.lookupAll(".table-cell").stream()
                .<TableCell<?, ?>>map(node -> (TableCell<?, ?>) node)
                .filter(cell -> cell.getTableColumn() != null && columnId.equals(cell.getTableColumn().getId()))
                .toList();
    }
}
