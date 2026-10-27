package pl.audiofix.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.audiofix.model.ConversionJob;
import pl.audiofix.model.ConversionMode;
import pl.audiofix.model.JobStatus;
import pl.audiofix.model.MediaInfo;
import pl.audiofix.model.StreamInfo;
import pl.audiofix.model.StreamType;
import pl.audiofix.model.TrackPlan;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.audiofix.ui.FxTestSupport.callOnFxThread;
import static pl.audiofix.ui.FxTestSupport.onFxThread;

/** Texts the code puts on screen, in English (#29): headers, table, statuses, progress, log, dialogs. */
class EnglishScenarioTest {

    private Parent root;
    private MainController controller;

    @BeforeAll
    static void english() {
        FxTestSupport.startJavaFx();
        Messages.use(Locale.ENGLISH);
    }

    @AfterAll
    static void backToDefault() {
        Messages.use(Locale.getDefault());
    }

    @BeforeEach
    void loadView() throws Exception {
        onFxThread(() -> {
            FXMLLoader loader = FxTestSupport.loadMainView();
            root = loader.getRoot();
            controller = loader.getController();
        });
    }

    @AfterEach
    void closeDialogs() throws Exception {
        onFxThread(() -> new ArrayList<>(Window.getWindows()).forEach(window -> {
            if (window instanceof Stage stage) {
                stage.close();
            }
        }));
    }

    // ---------------------------------------------------------------- main window

    @Test
    void emptyWindowIsInEnglish() throws Exception {
        assertEquals("File queue (0)", text("queueTitleLabel"));
        assertEquals("Remaining: 0", text("queueRemainingLabel"));
        assertEquals("Tracks", text("tracksTitleLabel"));
        assertEquals("Drop videos here or use [Add files…]",
                callOnFxThread(() -> ((Label) ((ListView<?>) root.lookup("#queueList")).getPlaceholder()).getText()));
        assertEquals("Add a file to see its tracks",
                callOnFxThread(() -> ((Label) ((TableView<?>) root.lookup("#trackTable")).getPlaceholder()).getText()));
    }

    @Test
    void loadedFileIsInEnglish() throws Exception {
        add();

        assertEquals("File queue (1)", text("queueTitleLabel"));
        assertEquals("Remaining: 1", text("queueRemainingLabel"));
        assertEquals("Tracks:", text("tracksTitleLabel"));
        assertEquals("4 tracks", text("trackCountLabel"));
    }

    @Test
    void trackTableIsInEnglish() throws Exception {
        add();

        assertEquals(List.of("Video", "Audio", "Audio", "Subtitles"), callOnFxThread(() -> cellTexts("typeColumn")));
        assertEquals("Copy", callOnFxThread(() -> fixedAction(0)));
        assertEquals("Copy", callOnFxThread(() -> fixedAction(3)));
    }

    @Test
    void logIsInEnglish() throws Exception {
        add();

        @SuppressWarnings("unchecked")
        ListView<String> log = (ListView<String>) root.lookup("#logList");
        assertTrue(callOnFxThread(() -> log.getItems().contains("Loaded Drive.mkv: 4 tracks")), callOnFxThread(() -> log.getItems().toString()));
        assertEquals(List.of("Copy", "Copy all"),
                callOnFxThread(() -> log.getContextMenu().getItems().stream().map(MenuItem::getText).toList()));
    }

    @Test
    void dialogIsInEnglish() throws Exception {
        onFxThread(() -> ((Button) root.lookup("#startButton")).fire());   // nothing to convert yet

        assertEquals(List.of("Cannot start the conversion\nNo files are waiting for conversion. Add files."), alertTexts());
    }

    // ---------------------------------------------------------------- messages from the model (#30)

    @Test
    void resultLinesOfTheLogAreInEnglish() throws Exception {
        add();
        ConversionJob job = callOnFxThread(() -> queueJobs().getFirst());

        onFxThread(() -> {
            job.markRunning();
            job.markFailed(pl.audiofix.model.UserMessage.of("ffmpeg.failed", "1", "Invalid data found"));
        });

        assertEquals("Error: ffmpeg exited with an error (code 1):\nInvalid data found", callOnFxThread(() -> job.getLog().getLast()));
    }

    @Test
    void doneLineIsInEnglish() throws Exception {
        add();
        ConversionJob job = callOnFxThread(() -> queueJobs().getFirst());

        onFxThread(() -> {
            job.markRunning();
            job.markDone();
        });

        assertEquals("Done: Drive_fixed.mkv", callOnFxThread(() -> job.getLog().getLast()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void actionListIsInEnglish() throws Exception {
        add();

        ComboBox<pl.audiofix.model.AudioCodec> action = (ComboBox<pl.audiofix.model.AudioCodec>) callOnFxThread(() -> root.lookupAll(".table-cell").stream()
                .map(node -> (TableCell<?, ?>) node)
                .filter(cell -> cell.getTableColumn() != null && "actionColumn".equals(cell.getTableColumn().getId()))
                .filter(cell -> cell.getIndex() == 2)
                .map(TableCell::getGraphic)
                .findFirst()
                .orElseThrow());

        assertEquals("Copy", action.getConverter().toString(pl.audiofix.model.AudioCodec.COPY));
        assertEquals("PCM 24-bit", action.getConverter().toString(pl.audiofix.model.AudioCodec.PCM_S24LE));
    }

    @Test
    void planProblemIsInEnglish() throws Exception {
        add();
        onFxThread(() -> {
            queueJobs().getFirst().getPlans().stream()
                    .filter(plan -> plan.getStream().isAudio())
                    .forEach(plan -> plan.setKeep(false));
            ((Button) root.lookup("#startButton")).fire();
        });

        assertEquals(List.of("Cannot start the conversion\nDrive.mkv: Keep at least one audio track."), alertTexts());
    }

    // ---------------------------------------------------------------- queue and progress texts

    @Test
    void queueStatusesAreInEnglish() {
        assertEquals("Waiting", QueueCell.statusText(JobStatus.PENDING, 0, ConversionMode.PLANNED));
        assertEquals("Converting 42% · quick", QueueCell.statusText(JobStatus.RUNNING, 0.42, ConversionMode.QUICK));
        assertEquals("Done", QueueCell.statusText(JobStatus.DONE, 1, ConversionMode.PLANNED));
        assertEquals("Failed", QueueCell.statusText(JobStatus.FAILED, 0, ConversionMode.PLANNED));
        assertEquals("Cancelled", QueueCell.statusText(JobStatus.CANCELLED, 0, ConversionMode.PLANNED));
    }

    @Test
    void progressIsInEnglish() {
        ConversionJob drive = ProgressTextsTest.drive(ConversionMode.PLANNED);

        assertEquals("Converting: tracks #1 and #3 (PCM 24-bit)", ProgressTexts.running(drive));
        assertEquals("track #4", ProgressTexts.tracks(List.of(4)));
        assertEquals("tracks #1, #2 and #3", ProgressTexts.tracks(List.of(1, 2, 3)));
        assertEquals("Quick conversion (PCM 24-bit)", ProgressTexts.running(ProgressTextsTest.drive(ConversionMode.QUICK)));
        assertEquals("42% (about 02:15 left)", ProgressTexts.percent(0.42, Optional.of(Duration.ofSeconds(135))));
        assertEquals("Done", ProgressTexts.finished(JobStatus.DONE));
    }

    @Test
    void copyingOnlyIsInEnglish() {
        ConversionJob drive = ProgressTextsTest.drive(ConversionMode.PLANNED);
        drive.getPlans().forEach(plan -> {
            if (plan.getStream().isAudio()) {
                plan.setTargetCodec(pl.audiofix.model.AudioCodec.COPY);
            }
        });

        assertEquals("Copying tracks", ProgressTexts.running(drive));
    }

    // ---------------------------------------------------------------- helpers

    private void add() throws Exception {
        MediaInfo drive = new MediaInfo(Path.of("D:\\Filmy\\Drive.mkv"), 6000, List.of(
                new StreamInfo(0, StreamType.VIDEO, "hevc", null, 0, null, "und", null, true),
                new StreamInfo(1, StreamType.AUDIO, "truehd", null, 8, "7.1", "eng", null, true),
                new StreamInfo(2, StreamType.AUDIO, "ac3", null, 6, "5.1", "eng", null, false),
                new StreamInfo(3, StreamType.SUBTITLE, "subrip", null, 0, null, "pol", null, false)));
        onFxThread(() -> {
            controller.addMedia(drive);
            FxTestSupport.layout(root);
        });
    }

    @SuppressWarnings("unchecked")
    private List<ConversionJob> queueJobs() {
        return ((ListView<ConversionJob>) root.lookup("#queueList")).getItems();
    }

    private String text(String id) throws Exception {
        return callOnFxThread(() -> ((Label) root.lookup("#" + id)).getText());
    }

    @SuppressWarnings("unchecked")
    private List<String> cellTexts(String columnId) {
        TableView<TrackPlan> table = (TableView<TrackPlan>) root.lookup("#trackTable");
        return table.lookupAll(".table-cell").stream()
                .map(node -> (TableCell<?, ?>) node)
                .filter(cell -> cell.getTableColumn() != null && columnId.equals(cell.getTableColumn().getId()))
                .filter(cell -> !cell.isEmpty())
                .sorted((a, b) -> Integer.compare(a.getIndex(), b.getIndex()))
                .map(TableCell::getText)
                .toList();
    }

    private String fixedAction(int row) {
        return root.lookupAll(".table-cell").stream()
                .map(node -> (TableCell<?, ?>) node)
                .filter(cell -> cell.getTableColumn() != null && "actionColumn".equals(cell.getTableColumn().getId()))
                .filter(cell -> cell.getIndex() == row)
                .map(cell -> (String) ((ComboBox<?>) cell.getGraphic()).getValue())
                .findFirst()
                .orElseThrow();
    }

    private static List<String> alertTexts() throws Exception {
        return callOnFxThread(() -> Window.getWindows().stream()
                .filter(window -> window.getScene() != null && window.getScene().getRoot() instanceof DialogPane)
                .map(window -> (DialogPane) window.getScene().getRoot())
                .map(pane -> pane.getHeaderText() + "\n" + pane.getContentText())
                .toList());
    }
}
