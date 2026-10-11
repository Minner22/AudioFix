package pl.audiofix.ui;

import javafx.css.PseudoClass;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.audiofix.ui.FxTestSupport.callOnFxThread;
import static pl.audiofix.ui.FxTestSupport.onFxThread;

/**
 * Track table of the main window, filled through MainController.addMedia
 * with streams like in the Drive remux (TrueHD, AC3, DTS-HD MA, subtitles, font attachment).
 */
class TrackTableTest {

    private static final PseudoClass NEEDS_CONVERSION = PseudoClass.getPseudoClass("needs-conversion");
    private static final PseudoClass REMOVED = PseudoClass.getPseudoClass("removed");
    private static final PseudoClass MISSING = PseudoClass.getPseudoClass("missing");

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
            controller.addMedia(DRIVE);
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
        onFxThread(() -> controller.addMedia(other()));

        assertEquals(2, table.getItems().size());
    }

    // ---------------------------------------------------------------- read-only cells

    @Test
    void indexIsSmallMutedNumber() {
        TableCell<?, ?> index = cell("indexColumn", 3);

        assertEquals("3", index.getText());
        assertTrue(index.getStyleClass().contains("track-index"));
    }

    @Test
    void typeShowsPolishLabelInItsColor() {
        assertType(0, "Wideo", "type-video");
        assertType(2, "Audio", "type-audio");
        assertType(4, "Napisy", "type-subtitle");
    }

    @Test
    void reusedTypeCellKeepsOnlyOneColor() throws Exception {
        // while scrolling the table moves a cell to another row; an old class left behind would mix the colors
        TableCell<?, ?> cell = reusedCell("typeColumn", 1, 4);   // TrueHD row, then the subtitles row

        assertEquals("Napisy", cell.getText());
        assertEquals(List.of("type-subtitle"), cell.getStyleClass().stream().filter(c -> c.startsWith("type-")).toList());
    }

    @Test
    void reusedLanguageBadgeKeepsOnlyOneColor() throws Exception {
        Label badge = (Label) reusedCell("languageColumn", 1, 4).getGraphic();   // eng, then pol

        assertEquals("pol", badge.getText());
        assertEquals(List.of("language-local"),
                badge.getStyleClass().stream().filter(c -> c.startsWith("language-") && !c.equals("language-badge")).toList());
    }

    @Test
    void codecHasNameAndDescriptionInTwoLines() {
        List<Label> lines = codecLines(1);

        assertEquals("truehd", lines.get(0).getText());
        assertEquals("Dolby TrueHD + Atmos 7.1", lines.get(1).getText());
        assertTrue(lines.get(0).getStyleClass().contains("codec-name"));
        assertTrue(lines.get(1).getStyleClass().contains("codec-description"));
        assertTrue(lines.get(1).isVisible());
    }

    @Test
    void videoCodecIsOneLineWithProfile() {
        List<Label> lines = codecLines(0);

        assertEquals("hevc (Main 10)", lines.get(0).getText());
        assertFalse(lines.get(1).isVisible(), "no second line for video");
        assertFalse(lines.get(1).isManaged(), "a hidden line must not take space");
    }

    @Test
    void channelsAreShownAsCount() {
        assertEquals("8 ch", cell("channelsColumn", 1).getText());
        assertEquals("6 ch", cell("channelsColumn", 3).getText());
    }

    @Test
    void channelsAreDashForVideoAndSubtitles() {
        for (int index : List.of(0, 4)) {
            TableCell<?, ?> channels = cell("channelsColumn", index);
            assertEquals("-", channels.getText());
            assertTrue(channels.getPseudoClassStates().contains(MISSING), "muted dash for stream " + index);
        }
    }

    @Test
    void languageIsBadgeColoredByLanguage() {
        assertBadge(1, "eng", "language-other");
        assertBadge(4, "pol", "language-local");
        assertBadge(0, "und", "language-unknown");
    }

    @Test
    void titleHasFullTextInTooltip() {
        TableCell<?, ?> title = cell("titleColumn", 1);

        assertEquals("TrueHD Atmos 7.1", title.getText());
        assertNotNull(title.getTooltip(), "long titles are cut, the tooltip shows them whole");
        assertEquals("TrueHD Atmos 7.1", title.getTooltip().getText());
    }

    @Test
    void missingTitleIsMutedDashWithoutTooltip() {
        TableCell<?, ?> title = cell("titleColumn", 4);

        assertEquals("-", title.getText());
        assertTrue(title.getPseudoClassStates().contains(MISSING));
        assertNull(title.getTooltip());
    }

    // ---------------------------------------------------------------- action column

    @Test
    void audioActionListIsAlwaysVisible() {
        ComboBox<?> action = actionIn(1);

        assertEquals(AudioCodec.PCM_S24LE, action.getValue());
        assertFalse(action.isDisabled());
        assertEquals(List.of(AudioCodec.values()), List.copyOf(action.getItems()));
    }

    @Test
    void videoAndSubtitlesShowLockedCopy() {
        ComboBox<?> video = actionIn(0);
        ComboBox<?> subtitles = actionIn(4);

        assertEquals("Kopiuj (passthrough)", video.getValue());
        assertEquals("Kopiuj", subtitles.getValue());
        assertTrue(video.isDisabled());
        assertTrue(subtitles.isDisabled());
    }

    @Test
    void lockedCopyIsOnlyShownNotPlanned() {
        // ffmpeg copies video and subtitles anyway; their plan keeps no target codec
        assertNull(plan(0).getTargetCodec());
        assertNull(plan(4).getTargetCodec());
    }

    @Test
    @SuppressWarnings("unchecked")
    void choosingActionChangesPlan() throws Exception {
        onFxThread(() -> ((ComboBox<AudioCodec>) actionIn(2)).setValue(AudioCodec.EAC3));

        assertEquals(AudioCodec.EAC3, plan(2).getTargetCodec());
    }

    @Test
    void changedPlanIsVisibleInActionList() throws Exception {
        onFxThread(() -> {
            plan(2).setTargetCodec(AudioCodec.AC3);
            FxTestSupport.layout(root);
        });

        assertEquals(AudioCodec.AC3, actionIn(2).getValue());
    }

    @Test
    void convertingActionIsOrange() throws Exception {
        assertTrue(actionIn(1).getStyleClass().contains("highlighted"), "TrueHD -> PCM");
        assertFalse(actionIn(2).getStyleClass().contains("highlighted"), "AC3 copied");

        onFxThread(() -> {
            plan(2).setTargetCodec(AudioCodec.EAC3);
            plan(1).setTargetCodec(AudioCodec.COPY);
            FxTestSupport.layout(root);
        });

        assertTrue(actionIn(2).getStyleClass().contains("highlighted"));
        assertFalse(actionIn(1).getStyleClass().contains("highlighted"));
    }

    @Test
    void removedTrackActionIsLocked() throws Exception {
        onFxThread(() -> plan(2).setKeep(false));
        assertTrue(actionIn(2).isDisabled());

        onFxThread(() -> plan(2).setKeep(true));
        assertFalse(actionIn(2).isDisabled());
    }

    // ---------------------------------------------------------------- keep column

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
    void keepCellStaysBrightInRemovedRow() {
        assertTrue(cell("keepColumn", 2).getStyleClass().contains("keep-cell"));
    }

    // ---------------------------------------------------------------- default column (#9)

    @Test
    void videoDefaultRadioIsLockedAndChecked() {
        RadioButton video = radioIn(cell("defaultColumn", 0));

        assertTrue(video.isSelected(), "video is always the default one");
        assertTrue(video.isDisabled());
    }

    @Test
    void defaultRadioReflectsPlan() {
        assertTrue(radioIn(cell("defaultColumn", 1)).isSelected(), "TrueHD is default in the file");
        assertFalse(radioIn(cell("defaultColumn", 2)).isSelected());
        assertFalse(radioIn(cell("defaultColumn", 4)).isSelected());
    }

    @Test
    void clickingAudioRadioMovesDefault() throws Exception {
        onFxThread(() -> {
            radioIn(cell("defaultColumn", 3)).fire();   // DTS
            FxTestSupport.layout(root);
        });

        assertTrue(plan(3).isMakeDefault());
        assertFalse(plan(1).isMakeDefault());
        assertTrue(radioIn(cell("defaultColumn", 3)).isSelected());
        assertFalse(radioIn(cell("defaultColumn", 1)).isSelected());
    }

    @Test
    void clickingSelectedAudioRadioKeepsItDefault() throws Exception {
        onFxThread(() -> {
            radioIn(cell("defaultColumn", 1)).fire();
            FxTestSupport.layout(root);
        });

        assertTrue(plan(1).isMakeDefault());
        assertTrue(radioIn(cell("defaultColumn", 1)).isSelected(), "audio always needs a default");
    }

    @Test
    void subtitleRadioTogglesOnAndOff() throws Exception {
        onFxThread(() -> {
            radioIn(cell("defaultColumn", 4)).fire();
            FxTestSupport.layout(root);
        });
        assertTrue(plan(4).isMakeDefault());

        onFxThread(() -> {
            radioIn(cell("defaultColumn", 4)).fire();
            FxTestSupport.layout(root);
        });
        assertFalse(plan(4).isMakeDefault());
        assertFalse(radioIn(cell("defaultColumn", 4)).isSelected());
    }

    @Test
    void unkeepingDefaultAudioMovesDefaultToFirstKeptAudio() throws Exception {
        onFxThread(() -> {
            checkBoxIn(cell("keepColumn", 1)).fire();   // user removes TrueHD
            FxTestSupport.layout(root);
        });

        assertFalse(plan(1).isMakeDefault());
        assertTrue(plan(2).isMakeDefault());
        assertTrue(radioIn(cell("defaultColumn", 2)).isSelected());
    }

    @Test
    void removedTrackCannotBeChosenAsDefault() throws Exception {
        onFxThread(() -> plan(3).setKeep(false));
        assertTrue(radioIn(cell("defaultColumn", 3)).isDisabled());

        onFxThread(() -> plan(3).setKeep(true));
        assertFalse(radioIn(cell("defaultColumn", 3)).isDisabled());
    }

    @Test
    void fileWithSeveralDefaultAudioShowsOnlyOneDefault() throws Exception {
        MediaInfo twoDefaults = new MediaInfo(Path.of("two.mkv"), 10, List.of(
                new StreamInfo(0, StreamType.VIDEO, "h264", null, 0, null, "und", null, true),
                new StreamInfo(1, StreamType.AUDIO, "dts", null, 6, null, "eng", null, true),
                new StreamInfo(2, StreamType.AUDIO, "ac3", null, 6, null, "pol", null, true)));

        onFxThread(() -> controller.addMedia(twoDefaults));

        assertTrue(plan(1).isMakeDefault());
        assertFalse(plan(2).isMakeDefault());
    }

    // ---------------------------------------------------------------- row styles

    @Test
    void tracksThatAreConvertedAreHighlighted() {
        assertTrue(hasPseudoClass(1, NEEDS_CONVERSION), "TrueHD");
        assertTrue(hasPseudoClass(3, NEEDS_CONVERSION), "DTS");
        assertFalse(hasPseudoClass(0, NEEDS_CONVERSION), "video");
        assertFalse(hasPseudoClass(2, NEEDS_CONVERSION), "AC3");
        assertFalse(hasPseudoClass(4, NEEDS_CONVERSION), "subtitles");
    }

    @Test
    void highlightFollowsChosenAction() throws Exception {
        onFxThread(() -> {
            plan(2).setTargetCodec(AudioCodec.EAC3);   // AC3 re-encoded
            plan(3).setTargetCodec(AudioCodec.COPY);   // DTS kept as is
            FxTestSupport.layout(root);
        });

        assertTrue(hasPseudoClass(2, NEEDS_CONVERSION), "AC3 -> E-AC3");
        assertFalse(hasPseudoClass(3, NEEDS_CONVERSION), "DTS -> Kopiuj");
    }

    @Test
    void removedTrackIsNotHighlighted() throws Exception {
        onFxThread(() -> {
            plan(3).setKeep(false);
            FxTestSupport.layout(root);
        });

        assertFalse(hasPseudoClass(3, NEEDS_CONVERSION), "nothing is converted in a removed track");
        assertTrue(hasPseudoClass(3, REMOVED));
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
        String css = new String(MainController.class.getResourceAsStream("theme/base.css").readAllBytes());

        assertTrue(css.contains(":needs-conversion"), "base.css has no :needs-conversion rule");
        assertTrue(css.contains(":removed"), "base.css has no :removed rule");
        assertTrue(css.contains(".highlighted"), "base.css has no rule for the orange action list");
    }

    // ---------------------------------------------------------------- helpers

    /** Video and one AAC track. */
    private static MediaInfo other() {
        return new MediaInfo(Path.of("other.mkv"), 10, List.of(
                new StreamInfo(0, StreamType.VIDEO, "h264", null, 0, null, "und", null, true),
                new StreamInfo(1, StreamType.AUDIO, "aac", "LC", 2, "stereo", "pol", null, true)));
    }

    private TrackPlan plan(int streamIndex) {
        return table.getItems().stream()
                .filter(p -> p.getStream().index() == streamIndex)
                .findFirst()
                .orElseThrow();
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

    /** A fresh cell of the column shown first in one row, then moved to another, as scrolling does. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private TableCell<?, ?> reusedCell(String columnId, int firstRow, int secondRow) throws Exception {
        return callOnFxThread(() -> {
            TableColumn column = column(columnId);
            TableCell cell = (TableCell) column.getCellFactory().call(column);
            TableRow<TrackPlan> row = new TableRow<>();
            row.updateTableView(table);
            cell.updateTableView(table);
            cell.updateTableColumn(column);
            cell.updateTableRow(row);
            row.updateIndex(firstRow);
            cell.updateIndex(firstRow);
            row.updateIndex(secondRow);
            cell.updateIndex(secondRow);
            return cell;
        });
    }

    private void assertType(int streamIndex, String text, String styleClass) {
        TableCell<?, ?> type = cell("typeColumn", streamIndex);
        assertEquals(text, type.getText());
        assertTrue(type.getStyleClass().contains(styleClass), type.getStyleClass().toString());
    }

    private void assertBadge(int streamIndex, String text, String styleClass) {
        Label badge = assertInstanceOf(Label.class, cell("languageColumn", streamIndex).getGraphic(), "no language badge");
        assertEquals(text, badge.getText());
        assertTrue(badge.getStyleClass().contains("language-badge"));
        assertTrue(badge.getStyleClass().contains(styleClass), badge.getStyleClass().toString());
    }

    private List<Label> codecLines(int streamIndex) {
        Node graphic = cell("codecColumn", streamIndex).getGraphic();
        assertNotNull(graphic, "codec cell shows no lines");
        try {
            return callOnFxThread(() -> graphic.lookupAll(".label").stream().map(Label.class::cast).toList());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private ComboBox<?> actionIn(int streamIndex) {
        return assertInstanceOf(ComboBox.class, cell("actionColumn", streamIndex).getGraphic(), "no action list");
    }

    private static CheckBox checkBoxIn(TableCell<?, ?> cell) {
        Node graphic = cell.getGraphic();
        assertNotNull(graphic, "keep cell has no checkbox");
        return (CheckBox) graphic;
    }

    private static RadioButton radioIn(TableCell<?, ?> cell) {
        Node graphic = cell.getGraphic();
        assertNotNull(graphic, "default cell has no radio button");
        return (RadioButton) graphic;
    }

    private boolean hasPseudoClass(int streamIndex, PseudoClass pseudoClass) {
        return row(streamIndex).getPseudoClassStates().contains(pseudoClass);
    }
}
