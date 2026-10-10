package pl.audiofix.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import pl.audiofix.ffmpeg.FfmpegPaths;
import pl.audiofix.ffmpeg.FfprobeService;
import pl.audiofix.ffmpeg.TestMedia;
import pl.audiofix.model.ConversionJob;
import pl.audiofix.model.MediaInfo;
import pl.audiofix.model.StreamInfo;
import pl.audiofix.model.TrackPlan;
import pl.audiofix.model.TrackPlans;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.audiofix.ui.FxTestSupport.onFxThread;

/**
 * End-to-end through the main window with the real ffmpeg:
 * load a file, press [Start], wait for the conversion and check the result with ffprobe.
 * Skipped when ffmpeg cannot be found.
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class ConversionTest {

    @TempDir
    static Path sampleDir;

    @TempDir
    Path dir;

    private static FfmpegPaths paths;
    private static Path sample;

    private MainController controller;
    private TableView<TrackPlan> table;
    private Button startButton;
    private Button quickConvertButton;
    private Button cancelButton;
    private Button addFilesButton;
    private ProgressBar progressBar;
    private TextArea logArea;
    private TextField outputField;
    private ListView<?> queueList;

    @BeforeAll
    static void setUpAll() throws Exception {
        paths = TestMedia.requireFfmpeg();
        sample = TestMedia.generateSample(paths.ffmpeg(), sampleDir);
        FxTestSupport.startJavaFx();
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void loadSample(TestInfo info) throws Exception {
        // own copy per test, so default output names do not collide between tests
        Path input = Files.copy(sample, dir.resolve(sample.getFileName()));

        onFxThread(() -> {
            FXMLLoader loader = FxTestSupport.loadMainView();
            Parent root = loader.getRoot();
            controller = loader.getController();
            controller.setFfmpegPaths(paths);
            table = (TableView<TrackPlan>) root.lookup("#trackTable");
            startButton = (Button) root.lookup("#startButton");
            quickConvertButton = (Button) root.lookup("#quickConvertButton");
            cancelButton = (Button) root.lookup("#cancelButton");
            addFilesButton = (Button) root.lookup("#addFilesButton");
            progressBar = (ProgressBar) root.lookup("#progressBar");
            logArea = (TextArea) root.lookup("#logArea");
            outputField = (TextField) root.lookup("#outputField");
            queueList = (ListView<?>) root.lookup("#queueList");

            controller.addFiles(List.of(input));   // same path as [Dodaj pliki…]: ffprobe in a background Task
        });
        waitUntil(() -> table.getItems().size() == 4, "file was not loaded");
    }

    @AfterEach
    void closeDialogs() throws Exception {
        // alerts (info / warning / error) are real windows - close them after each test
        onFxThread(() -> new ArrayList<>(Window.getWindows()).forEach(window -> {
            if (window instanceof Stage stage) {
                stage.close();
            }
        }));
    }

    // ---------------------------------------------------------------- success

    @Test
    void convertsLoadedFileWithDefaultPlan() throws Exception {
        Path output = outputPath();

        onFxThread(startButton::fire);
        waitUntilFinished();

        assertTrue(Files.isRegularFile(output), "no output file " + output);
        MediaInfo result = new FfprobeService(paths).probe(output);
        assertEquals(List.of("mpeg4", "pcm_s24le", "ac3", "subrip"),
                result.streams().stream().map(StreamInfo::codec).toList());
        assertEquals(1.0, progressBar.getProgress());
    }

    @Test
    void tableChangesEndUpInOutputFile() throws Exception {
        Path output = outputPath();
        onFxThread(() -> {
            plan(2).setKeep(false);                              // drop AC3
            TrackPlans.setDefault(table.getItems(), plan(3));    // subtitles become default
        });

        onFxThread(startButton::fire);
        waitUntilFinished();

        MediaInfo result = new FfprobeService(paths).probe(output);
        assertEquals(List.of("mpeg4", "pcm_s24le", "subrip"),
                result.streams().stream().map(StreamInfo::codec).toList());
        assertTrue(result.streams().get(1).isDefault(), "audio should stay default");
        assertTrue(result.streams().get(2).isDefault(), "subtitles should be default");
    }

    @Test
    void commandAndFfmpegOutputAreLogged() throws Exception {
        onFxThread(startButton::fire);
        waitUntilFinished();

        String log = logArea.getText();
        assertTrue(log.contains("pcm_s24le"), "ffmpeg command not in log:\n" + log);
        assertTrue(log.contains("\"" + outputPath() + "\""), "output path with spaces should be quoted:\n" + log);
        assertTrue(log.contains("Stream mapping"), "ffmpeg stderr not in log:\n" + log);
    }

    // ---------------------------------------------------------------- UI state

    @Test
    void controlsAreLockedWhileConvertingAndUnlockedAfter() throws Exception {
        AtomicBoolean lockedRightAfterStart = new AtomicBoolean();
        onFxThread(() -> {
            startButton.fire();
            lockedRightAfterStart.set(startButton.isDisable()
                    && !cancelButton.isDisable()
                    && addFilesButton.isDisable());
        });
        assertTrue(lockedRightAfterStart.get(), "controls should be locked while converting");

        // the table locks when the shown file itself starts converting (other waiting files stay editable)
        waitUntil(() -> table.isDisable() || !startButton.isDisable(), "table was not locked");
        waitUntilFinished();

        assertFalse(startButton.isDisable());
        assertTrue(cancelButton.isDisable());
        assertFalse(addFilesButton.isDisable());
        assertTrue(table.isDisable(), "a converted file cannot be edited any more");
    }

    @Test
    void convertedFileCanBeAddedAndConvertedAgain() throws Exception {
        Path first = outputPath();
        onFxThread(startButton::fire);
        waitUntilFinished();

        onFxThread(() -> controller.addFiles(List.of(dir.resolve(sample.getFileName()))));
        waitUntil(() -> queueList.getItems().size() == 2, "file was not added again");
        Path second = outputPath();
        onFxThread(startButton::fire);
        waitUntilFinished();

        assertTrue(Files.isRegularFile(first));
        assertTrue(Files.isRegularFile(second));
        assertTrue(second.getFileName().toString().endsWith("_fixed (1).mkv"), "first output must not be overwritten: " + second);
    }

    @Test
    void startWithNothingWaitingDoesNotRunAnything() throws Exception {
        onFxThread(startButton::fire);
        waitUntilFinished();
        Path output = outputPath();
        long modified = Files.getLastModifiedTime(output).toMillis();

        onFxThread(startButton::fire);

        assertFalse(startButton.isDisable(), "the only file is done - nothing to start");
        assertEquals(modified, Files.getLastModifiedTime(output).toMillis());
    }

    // ---------------------------------------------------------------- several files (#13)

    @Test
    void queueConvertsEveryFileWithItsOwnSettings() throws Exception {
        Path firstOutput = outputPath();
        onFxThread(() -> plan(2).setKeep(false));   // first file: drop AC3
        Path second = Files.copy(sample, dir.resolve("Odcinek 2.mkv"));
        onFxThread(() -> controller.addFiles(List.of(second)));
        waitUntil(() -> queueList.getItems().size() == 2, "second file was not added");
        Path secondOutput = outputPath();

        onFxThread(startButton::fire);
        waitUntilFinished();

        FfprobeService probe = new FfprobeService(paths);
        assertEquals(List.of("mpeg4", "pcm_s24le", "subrip"), codecs(probe.probe(firstOutput)));
        assertEquals(List.of("mpeg4", "pcm_s24le", "ac3", "subrip"), codecs(probe.probe(secondOutput)));
        assertTrue(alertTexts().stream().anyMatch(text -> text.contains("Gotowe: 2")), alertTexts().toString());
    }

    @Test
    void quickConversionConvertsAllWaitingFiles() throws Exception {
        Path firstOutput = outputPath();
        Path second = Files.copy(sample, dir.resolve("Odcinek 2.mkv"));
        onFxThread(() -> controller.addFiles(List.of(second)));
        waitUntil(() -> queueList.getItems().size() == 2, "second file was not added");
        Path secondOutput = outputPath();

        onFxThread(quickConvertButton::fire);
        waitUntilFinished();

        FfprobeService probe = new FfprobeService(paths);
        assertEquals(List.of("mpeg4", "pcm_s24le", "ass"), codecs(probe.probe(firstOutput)));
        assertEquals(List.of("mpeg4", "pcm_s24le", "ass"), codecs(probe.probe(secondOutput)));
    }

    @Test
    void summaryListsFailedFilesAndQueueGoesOn() throws Exception {
        Path second = Files.copy(sample, dir.resolve("Odcinek 2.mkv"));
        onFxThread(() -> controller.addFiles(List.of(second)));
        waitUntil(() -> queueList.getItems().size() == 2, "second file was not added");
        Path secondOutput = outputPath();
        Files.delete(dir.resolve(sample.getFileName()));   // first file disappears after loading

        onFxThread(startButton::fire);
        waitUntilFinished();

        assertTrue(Files.isRegularFile(secondOutput), "second file should still be converted");
        String summary = String.join("\n", alertTexts());
        assertTrue(summary.contains("Konwersja zakończona z błędami"), summary);
        assertTrue(summary.contains("Gotowe: 1"), summary);
        assertTrue(summary.contains("Błędy: 1"), summary);
        assertTrue(summary.contains(sample.getFileName().toString()), "failed file should be named: " + summary);
    }

    @Test
    void severalFilesCanBeAddedAtOnce() throws Exception {
        Path second = Files.copy(sample, dir.resolve("Odcinek 2.mkv"));
        Path third = Files.copy(sample, dir.resolve("Odcinek 3.mkv"));

        onFxThread(() -> controller.addFiles(List.of(second, third)));
        waitUntil(() -> queueList.getItems().size() == 3, "files were not added");

        assertEquals(List.of(sample.getFileName().toString(), "Odcinek 2.mkv", "Odcinek 3.mkv"),
                queueList.getItems().stream().map(job -> ((ConversionJob) job).getInput().getFileName().toString()).toList());
    }

    @Test
    void unreadableFileIsReportedAndOthersAreAdded() throws Exception {
        Path broken = Files.writeString(dir.resolve("Uszkodzony.mkv"), "to nie jest film");
        Path second = Files.copy(sample, dir.resolve("Odcinek 2.mkv"));

        onFxThread(() -> controller.addFiles(List.of(broken, second)));
        waitUntil(() -> queueList.getItems().size() == 2 && !addFilesButton.isDisable(), "second file was not added");

        assertTrue(alertTexts().stream().anyMatch(text -> text.contains("Uszkodzony.mkv")), alertTexts().toString());
    }

    // ---------------------------------------------------------------- drag and drop (#49)

    @Test
    void droppedFolderAddsItsFilmsSortedByName() throws Exception {
        Path season = Files.createDirectory(dir.resolve("Sezon 1"));
        Files.copy(sample, season.resolve("Odcinek 2.mkv"));
        Files.copy(sample, season.resolve("Odcinek 1.mkv"));
        Files.writeString(season.resolve("Odcinek 1.srt"), "1");

        boolean added = FxTestSupport.callOnFxThread(() -> controller.dropFiles(List.of(season)));
        waitUntil(() -> queueList.getItems().size() == 3 && !addFilesButton.isDisable(), "films were not added");

        assertTrue(added);
        assertEquals(List.of(sample.getFileName().toString(), "Odcinek 1.mkv", "Odcinek 2.mkv"),
                queueList.getItems().stream().map(job -> ((ConversionJob) job).getInput().getFileName().toString()).toList());
        assertTrue(alertTexts().isEmpty(), "subtitles inside a folder are skipped silently: " + alertTexts());
    }

    @Test
    void droppedFilmAndOtherFileAddsFilmAndNamesTheOther() throws Exception {
        Path film = Files.copy(sample, dir.resolve("Odcinek 2.mkv"));
        Path notes = Files.writeString(dir.resolve("notatki.txt"), "x");

        onFxThread(() -> controller.dropFiles(List.of(notes, film)));
        waitUntil(() -> queueList.getItems().size() == 2 && !addFilesButton.isDisable(), "film was not added");

        assertTrue(alertTexts().stream().anyMatch(text -> text.contains("notatki.txt")), alertTexts().toString());
    }

    @Test
    void dropDuringConversionIsIgnored() throws Exception {
        Path film = Files.copy(sample, dir.resolve("Odcinek 2.mkv"));
        AtomicBoolean added = new AtomicBoolean(true);

        onFxThread(() -> {
            startButton.fire();
            added.set(controller.dropFiles(List.of(film)));
        });
        waitUntilFinished();

        assertFalse(added.get(), "drop must be refused like [Dodaj pliki…] while converting");
        assertEquals(1, queueList.getItems().size());
    }

    // ---------------------------------------------------------------- quick conversion (#21)

    @Test
    void quickConversionIgnoresTableAndKeepsOneAudioTrack() throws Exception {
        Path output = outputPath();
        onFxThread(() -> plan(1).setKeep(false));   // table changes do not matter for quick conversion

        onFxThread(quickConvertButton::fire);
        waitUntilFinished();

        MediaInfo result = new FfprobeService(paths).probe(output);
        assertEquals(List.of("mpeg4", "pcm_s24le", "ass"),
                result.streams().stream().map(StreamInfo::codec).toList());
        assertEquals(1.0, progressBar.getProgress());
    }

    @Test
    void quickConversionLocksControlsLikeStart() throws Exception {
        AtomicBoolean locked = new AtomicBoolean();
        onFxThread(() -> {
            quickConvertButton.fire();
            locked.set(quickConvertButton.isDisable() && startButton.isDisable()
                    && !cancelButton.isDisable() && addFilesButton.isDisable());
        });
        assertTrue(locked.get(), "controls should be locked during quick conversion");

        waitUntilFinished();

        assertFalse(quickConvertButton.isDisable());
        assertFalse(startButton.isDisable());
    }

    @Test
    void quickConversionLogsItsCommand() throws Exception {
        onFxThread(quickConvertButton::fire);
        waitUntilFinished();

        String log = logArea.getText();
        assertTrue(log.contains("-c:a pcm_s24le"), "quick command not in log:" + System.lineSeparator() + log);
        assertFalse(log.contains("-map "), "quick command must not map streams:" + System.lineSeparator() + log);
    }

    // ---------------------------------------------------------------- errors

    @Test
    void invalidPlanDoesNotStartConversion() throws Exception {
        Path output = outputPath();
        onFxThread(() -> table.getItems().stream()
                .filter(p -> p.getStream().isAudio())
                .forEach(p -> p.setKeep(false)));

        onFxThread(startButton::fire);

        assertFalse(startButton.isDisable(), "conversion must not start without audio");
        assertFalse(Files.exists(output));
    }

    @Test
    void failedConversionUnlocksUiAndLeavesNoFile() throws Exception {
        Path output = outputPath();
        Files.delete(dir.resolve(sample.getFileName()));   // source disappears after loading

        onFxThread(startButton::fire);
        waitUntilFinished();

        assertFalse(Files.exists(output), "partial output should be deleted");
        assertEquals(0.0, progressBar.getProgress());
    }

    // ---------------------------------------------------------------- helpers

    private static List<String> codecs(MediaInfo info) {
        return info.streams().stream().map(StreamInfo::codec).toList();
    }

    /** Header and content of every open alert. */
    private static List<String> alertTexts() throws Exception {
        return FxTestSupport.callOnFxThread(() -> Window.getWindows().stream()
                .filter(window -> window.getScene() != null && window.getScene().getRoot() instanceof DialogPane)
                .map(window -> (DialogPane) window.getScene().getRoot())
                .map(pane -> pane.getHeaderText() + "\n" + pane.getContentText())
                .toList());
    }

    private Path outputPath() {
        return Path.of(outputField.getText());
    }

    private TrackPlan plan(int streamIndex) {
        return table.getItems().stream()
                .filter(p -> p.getStream().index() == streamIndex)
                .findFirst()
                .orElseThrow();
    }

    private void waitUntilFinished() throws Exception {
        waitUntil(() -> !startButton.isDisable() && cancelButton.isDisable(), "conversion did not finish");
    }

    /** Polls the condition on the JavaFX Application Thread. */
    private static void waitUntil(BooleanSupplier condition, String message) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        AtomicBoolean done = new AtomicBoolean();
        while (System.nanoTime() < deadline) {
            onFxThread(() -> done.set(condition.getAsBoolean()));
            if (done.get()) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError(message);
    }
}
