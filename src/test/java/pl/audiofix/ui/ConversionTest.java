package pl.audiofix.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
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
    private Button cancelButton;
    private Button addFilesButton;
    private ProgressBar progressBar;
    private TextArea logArea;
    private TextField outputField;

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
            cancelButton = (Button) root.lookup("#cancelButton");
            addFilesButton = (Button) root.lookup("#addFilesButton");
            progressBar = (ProgressBar) root.lookup("#progressBar");
            logArea = (TextArea) root.lookup("#logArea");
            outputField = (TextField) root.lookup("#outputField");

            controller.loadFile(input);   // same path as [Dodaj pliki…]: ffprobe in a background Task
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
                    && addFilesButton.isDisable()
                    && table.isDisable());
        });
        assertTrue(lockedRightAfterStart.get(), "controls should be locked while converting");

        waitUntilFinished();

        assertFalse(startButton.isDisable());
        assertTrue(cancelButton.isDisable());
        assertFalse(addFilesButton.isDisable());
        assertFalse(table.isDisable());
    }

    @Test
    void conversionCanBeStartedAgain() throws Exception {
        onFxThread(startButton::fire);
        waitUntilFinished();

        onFxThread(startButton::fire);
        waitUntilFinished();

        assertTrue(Files.isRegularFile(outputPath()));
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
