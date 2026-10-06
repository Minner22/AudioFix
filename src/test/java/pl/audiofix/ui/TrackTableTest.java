package pl.audiofix.ui;

import javafx.css.PseudoClass;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.audiofix.model.AudioCodec;
import pl.audiofix.model.MediaInfo;
import pl.audiofix.model.StreamInfo;
import pl.audiofix.model.StreamType;
import pl.audiofix.model.TrackPlan;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.audiofix.ui.FxTestSupport.onFxThread;

/**
 * Track table of the main window, filled through MainController.showMedia
 * with streams like in the Drive remux (TrueHD, AC3, DTS-HD MA, subtitles, font attachment).
 */
class TrackTableTest {

    private static final PseudoClass NEEDS_CONVERSION = PseudoClass.getPseudoClass("needs-conversion");
    private static final PseudoClass REMOVED = PseudoClass.getPseudoClass("removed");

    // 0 video, 1 TrueHD (default), 2 AC3, 3 DTS-HD MA, 4 subtitles, 5 font attachment
    private static final MediaInfo DRIVE = new MediaInfo(Path.of("D:\\Filmy\\Drive.mkv"), 6000, List.of(
            new StreamInfo(0, StreamType.VIDEO, "hevc", "Main 10", 0, null, "und", null, true),
            new StreamInfo(1, StreamType.AUDIO, "truehd", "Dolby TrueHD + Dolby Atmos", 8, "7.1", "eng", "TrueHD Atmos 7.1", true),
            new StreamInfo(2, StreamType.AUDIO, "ac3", null, 6, "5.1(side)", "eng", "AC-3 5.1", false),
            new StreamInfo(3, StreamType.AUDIO, "dts", "DTS-HD MA", 6, "5.1(side)", "eng", "DTS-HD MA 5.1", false),
            new StreamInfo(4, StreamType.SUBTITLE, "hdmv_pgs_subtitle", null, 0, null, "pol", null, false),
            new StreamInfo(5, StreamType.ATTACHMENT, "ttf", null, 0, null, "und", "font.ttf", false)));

    private Parent root;
    private TableView<TrackPlan> table;
    private MainController controller;

    @BeforeAll
    static void startJavaFx() {
        FxTestSupport.startJavaFx();
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void showDrive() throws Exception {
        onFxThread(() -> {
            FXMLLoader loader = FxTestSupport.loadMainView();
            root = loader.getRoot();
            table = (TableView<TrackPlan>) root.lookup("#trackTable");
            controller = loader.getController();
            controller.showMedia(DRIVE);
            FxTestSupport.layout(root);
        });
    }

    // ---------------------------------------------------------------- rows

    @Test
    void showsVideoAudioAndSubtitlesButNotAttachments() {
        assertEquals(List.of(0, 1, 2, 3, 4), table.getItems().stream().map(p -> p.getStream().index()).toList());
    }

    @Test
    void rowsUseDefaultPlans() {
        assertEquals(AudioCodec.PCM_S24LE, plan(1).getTargetCodec());   // TrueHD
        assertEquals(AudioCodec.COPY, plan(2).getTargetCodec());        // AC3
        assertEquals(AudioCodec.PCM_S24LE, plan(3).getTargetCodec());   // DTS
        assertTrue(table.getItems().stream().allMatch(TrackPlan::isKeep));
    }

    @Test
    void showingAnotherFileReplacesRows() throws Exception {
        MediaInfo other = new MediaInfo(Path.of("other.mkv"), 10, List.of(
                new StreamInfo(0, StreamType.VIDEO, "h264", null, 0, null, "und", null, true),
                new StreamInfo(1, StreamType.AUDIO, "aac", "LC", 2, "stereo", "pol", null, true)));

        onFxThread(() -> controller.showMedia(other));

        assertEquals(2, table.getItems().size());
    }

    // ---------------------------------------------------------------- columns

    @Test
    void readOnlyColumnsShowStreamData() {
        int trueHd = 1;
        assertEquals(1, cellData("indexColumn", trueHd));
        assertEquals("Audio", cellData("typeColumn", trueHd));
        assertEquals("truehd (Dolby TrueHD + Dolby Atmos)", cellData("codecColumn", trueHd));
        assertEquals(8, cellData("channelsColumn", trueHd));
        assertEquals("eng", cellData("languageColumn", trueHd));
        assertEquals("TrueHD Atmos 7.1", cellData("titleColumn", trueHd));
    }

    @Test
    void typeColumnUsesPolishLabels() {
        assertEquals("Wideo", cellData("typeColumn", 0));
        assertEquals("Audio", cellData("typeColumn", 2));
        assertEquals("Napisy", cellData("typeColumn", 4));
    }

    @Test
    void channelsAreEmptyForNonAudio() {
        assertNull(cellData("channelsColumn", 0));
        assertNull(cellData("channelsColumn", 4));
    }

    @Test
    void missingTitleIsEmpty() {
        assertNull(cellData("titleColumn", 4));
    }

    @Test
    void actionColumnShowsTargetCodecOnlyForAudio() {
        assertEquals(AudioCodec.PCM_S24LE, cellData("actionColumn", 1));
        assertNull(cellData("actionColumn", 0));
        assertNull(cellData("actionColumn", 4));
    }

    // ---------------------------------------------------------------- editing

    @Test
    void tableIsEditable() {
        assertTrue(table.isEditable());
    }

    @Test
    void keepCanBeChangedForAudioAndSubtitlesButNotVideo() {
        assertFalse(cell("keepColumn", 0).isEditable(), "video must always be kept");
        assertTrue(cell("keepColumn", 1).isEditable());
        assertTrue(cell("keepColumn", 4).isEditable());
    }

    @Test
    void keepCheckboxIsBoundToPlan() throws Exception {
        CheckBox checkBox = checkBoxIn(cell("keepColumn", 2));
        assertTrue(checkBox.isSelected());

        onFxThread(checkBox::fire);   // user unchecks AC3

        assertFalse(plan(2).isKeep());
    }

    @Test
    void videoKeepCheckboxIsDisabled() {
        assertTrue(checkBoxIn(cell("keepColumn", 0)).isDisabled());
    }

    @Test
    void actionCanBeChangedOnlyForAudio() {
        assertFalse(cell("actionColumn", 0).isEditable());
        assertTrue(cell("actionColumn", 1).isEditable());
        assertFalse(cell("actionColumn", 4).isEditable());
    }

    @Test
    void changedPlanIsVisibleInActionColumn() throws Exception {
        onFxThread(() -> plan(2).setTargetCodec(AudioCodec.EAC3));

        assertEquals(AudioCodec.EAC3, cellData("actionColumn", 2));
    }

    // ---------------------------------------------------------------- row styles

    @Test
    void tracksThatNeedConversionAreHighlighted() {
        assertTrue(hasPseudoClass(1, NEEDS_CONVERSION), "TrueHD");
        assertTrue(hasPseudoClass(3, NEEDS_CONVERSION), "DTS");
        assertFalse(hasPseudoClass(0, NEEDS_CONVERSION), "video");
        assertFalse(hasPseudoClass(2, NEEDS_CONVERSION), "AC3");
        assertFalse(hasPseudoClass(4, NEEDS_CONVERSION), "subtitles");
    }

    @Test
    void removedTrackIsGreyedOutAndBackWhenKeptAgain() throws Exception {
        onFxThread(() -> {
            plan(2).setKeep(false);
            FxTestSupport.layout(root);
        });
        assertTrue(hasPseudoClass(2, REMOVED));

        onFxThread(() -> {
            plan(2).setKeep(true);
            FxTestSupport.layout(root);
        });
        assertFalse(hasPseudoClass(2, REMOVED));
    }

    @Test
    void stylesheetDefinesRowStyles() throws Exception {
        String css = new String(MainController.class.getResourceAsStream("styles.css").readAllBytes());

        assertTrue(css.contains(":needs-conversion"), "styles.css has no :needs-conversion rule");
        assertTrue(css.contains(":removed"), "styles.css has no :removed rule");
    }

    // ---------------------------------------------------------------- helpers

    private TrackPlan plan(int streamIndex) {
        return table.getItems().stream()
                .filter(p -> p.getStream().index() == streamIndex)
                .findFirst()
                .orElseThrow();
    }

    private Object cellData(String columnId, int streamIndex) {
        return column(columnId).getCellData(plan(streamIndex));
    }

    private TableColumn<TrackPlan, ?> column(String columnId) {
        return table.getColumns().stream()
                .filter(c -> columnId.equals(c.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no column " + columnId));
    }

    private TableRow<?> row(int streamIndex) {
        TrackPlan plan = plan(streamIndex);
        return table.lookupAll(".table-row-cell").stream()
                .map(n -> (TableRow<?>) n)
                .filter(r -> r.getItem() == plan)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no row for stream " + streamIndex));
    }

    private TableCell<?, ?> cell(String columnId, int streamIndex) {
        TableColumn<TrackPlan, ?> column = column(columnId);
        return row(streamIndex).lookupAll(".table-cell").stream()
                .map(n -> (TableCell<?, ?>) n)
                .filter(c -> c.getTableColumn() == column)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no " + columnId + " cell for stream " + streamIndex));
    }

    private static CheckBox checkBoxIn(TableCell<?, ?> cell) {
        Node graphic = cell.getGraphic();
        assertNotNull(graphic, "keep cell has no checkbox");
        return (CheckBox) graphic;
    }

    private boolean hasPseudoClass(int streamIndex, PseudoClass pseudoClass) {
        return row(streamIndex).getPseudoClassStates().contains(pseudoClass);
    }
}
