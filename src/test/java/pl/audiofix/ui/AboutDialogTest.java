package pl.audiofix.ui;

import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.audiofix.AppVersion;
import pl.audiofix.ffmpeg.FfmpegPaths;
import pl.audiofix.ffmpeg.TestMedia;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.audiofix.ui.FxTestSupport.onFxThread;

/**
 * Opening the "O programie" window the way [O programie] does: FXML loading, controller wiring
 * and the ffmpeg version filled in from the background task.
 */
class AboutDialogTest {

    @TempDir
    Path dir;

    private final AtomicReference<Stage> stage = new AtomicReference<>();

    @BeforeAll
    static void startJavaFx() {
        FxTestSupport.startJavaFx();
    }

    @AfterEach
    void closeWindow() throws Exception {
        onFxThread(() -> {
            if (stage.get() != null) {
                stage.get().close();
            }
        });
    }

    @Test
    void opensWindowWithAppVersionAndTitle() throws Exception {
        FfmpegPaths missing = FfmpegPaths.fromFfmpeg(dir.resolve("ffmpeg.exe"));

        onFxThread(() -> stage.set(AboutDialog.show(null, missing, null)));

        assertTrue(stage.get().isShowing());
        assertEquals("O programie - AudioFix", stage.get().getTitle());
        assertEquals(Modality.WINDOW_MODAL, stage.get().getModality());
        assertFalse(stage.get().isResizable());
        assertEquals("AudioFix " + AppVersion.current(), label("appVersionLabel").getText());
    }

    @Test
    void realFfmpegVersionReplacesPendingText() throws Exception {
        FfmpegPaths paths = TestMedia.requireFfmpeg();

        onFxThread(() -> stage.set(AboutDialog.show(null, paths, null)));
        waitUntil(() -> label("ffmpegLabel").getText().contains("ffmpeg version "),
                "ffmpeg version was not filled in");

        String text = label("ffmpegLabel").getText();
        assertFalse(text.contains("sprawdzanie"), text);
        assertTrue(text.contains(paths.ffmpeg().toString()), text);
    }

    @Test
    void unreadableFfmpegEndsAsUnknownInsteadOfPending() throws Exception {
        FfmpegPaths missing = FfmpegPaths.fromFfmpeg(dir.resolve("ffmpeg.exe"));

        onFxThread(() -> stage.set(AboutDialog.show(null, missing, null)));
        waitUntil(() -> !label("ffmpegLabel").getText().contains("sprawdzanie"),
                "label stayed on the pending text");

        assertTrue(label("ffmpegLabel").getText().contains("nieznana"), label("ffmpegLabel").getText());
    }

    // ---------------------------------------------------------------- helpers

    private Label label(String id) {
        Label label = (Label) stage.get().getScene().getRoot().lookup("#" + id);
        assertNotNull(label, "no label " + id);
        return label;
    }

    /** Polls the condition on the JavaFX Application Thread. */
    private static void waitUntil(BooleanSupplier condition, String message) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
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
