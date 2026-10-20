package pl.audiofix.ui;

import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.audiofix.model.AudioCodec;
import pl.audiofix.model.ConversionJob;
import pl.audiofix.model.ConversionMode;
import pl.audiofix.model.MediaInfo;
import pl.audiofix.model.UserMessage;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.audiofix.ui.FxTestSupport.onFxThread;

/** The bottom bar while a file runs and after it ends, with a fake clock. */
class ProgressViewTest {

    private final AtomicLong now = new AtomicLong();
    private ProgressBar bar;
    private Label status;
    private Label percent;
    private ProgressView view;

    @BeforeAll
    static void startJavaFx() {
        FxTestSupport.startJavaFx();
    }

    @BeforeEach
    void createView() throws Exception {
        onFxThread(() -> {
            bar = new ProgressBar(0);
            status = new Label("x");
            percent = new Label("x");
            view = new ProgressView(bar, status, percent, now::get);
        });
    }

    @Test
    void emptyBeforeAnyFile() {
        assertEquals("", status.getText());
        assertEquals("", percent.getText());
    }

    @Test
    void runningFileShowsWhatIsConverted() throws Exception {
        ConversionJob drive = running(ProgressTextsTest.drive(ConversionMode.PLANNED));

        assertEquals("Konwertowanie: ścieżki #1 i #3 (PCM 24-bit)", status.getText());
        assertEquals("0%", percent.getText());
    }

    @Test
    void percentAndTimeLeftFollowProgress() throws Exception {
        ConversionJob drive = running(ProgressTextsTest.drive(ConversionMode.PLANNED));

        onFxThread(() -> {
            now.set(Duration.ofSeconds(98).toNanos());
            drive.setProgress(0.42);
        });

        assertEquals("42% (Pozostało ok. 02:15)", percent.getText());
        assertEquals(0.42, bar.getProgress(), 1e-9);
    }

    @Test
    void timeLeftWaitsForFirstSeconds() throws Exception {
        ConversionJob drive = running(ProgressTextsTest.drive(ConversionMode.PLANNED));

        onFxThread(() -> {
            now.set(Duration.ofSeconds(2).toNanos());
            drive.setProgress(0.05);
        });

        assertEquals("5%", percent.getText());
    }

    @Test
    void unknownLengthShowsMovingBarWithoutNumbers() throws Exception {
        ConversionJob job = ProgressTextsTest.drive(ConversionMode.PLANNED);
        ConversionJob noLength = new ConversionJob(new MediaInfo(job.getInput(), 0, job.getMediaInfo().streams()),
                job.getPlans(), job.getOutput(), ConversionMode.PLANNED);
        running(noLength);

        assertEquals(ProgressBar.INDETERMINATE_PROGRESS, bar.getProgress());
        assertEquals("", percent.getText());
        assertFalse(status.getText().isEmpty(), "what is converted is still known");
    }

    @Test
    void doneFileSaysDoneWithFullBar() throws Exception {
        ConversionJob drive = running(ProgressTextsTest.drive(ConversionMode.PLANNED));

        onFxThread(drive::markDone);

        assertEquals("Gotowe", status.getText());
        assertEquals("", percent.getText());
        assertEquals(1.0, bar.getProgress());
    }

    @Test
    void cancelledAndFailedFilesSaySo() throws Exception {
        ConversionJob cancelled = running(ProgressTextsTest.drive(ConversionMode.PLANNED));
        onFxThread(cancelled::markCancelled);
        assertEquals("Anulowano", status.getText());

        ConversionJob failed = running(ProgressTextsTest.drive(ConversionMode.PLANNED));
        onFxThread(() -> failed.markFailed(UserMessage.of("ffmpeg.failed", "1", "Invalid data")));
        assertEquals("Błąd", status.getText());
    }

    @Test
    void nextFileReplacesPreviousOne() throws Exception {
        ConversionJob first = running(ProgressTextsTest.drive(ConversionMode.PLANNED));
        running(ProgressTextsTest.drive(ConversionMode.QUICK));

        onFxThread(() -> first.setProgress(0.9));   // the previous file must not move the bar any more

        assertEquals("Szybka konwersja (PCM 24-bit)", status.getText());
        assertEquals(0.0, bar.getProgress());
    }

    @Test
    void timeLeftStartsAgainForNextFile() throws Exception {
        ConversionJob first = running(ProgressTextsTest.drive(ConversionMode.PLANNED));
        onFxThread(() -> {
            now.set(Duration.ofSeconds(600).toNanos());
            first.setProgress(0.99);
            first.markDone();
        });

        ConversionJob second = running(ProgressTextsTest.drive(ConversionMode.PLANNED));
        onFxThread(() -> {
            now.set(Duration.ofSeconds(660).toNanos());   // 60 s after the second file started
            second.setProgress(0.5);
        });

        assertEquals("50% (Pozostało ok. 01:00)", percent.getText());
    }

    @Test
    void barIsOrangeOnlyWhenAudioIsConverted() throws Exception {
        running(ProgressTextsTest.drive(ConversionMode.PLANNED));
        assertTrue(bar.getPseudoClassStates().contains(ProgressView.CONVERTING));

        ConversionJob copyOnly = ProgressTextsTest.drive(ConversionMode.PLANNED);
        copyOnly.getPlans().forEach(plan -> {
            if (plan.getStream().isAudio()) {
                plan.setTargetCodec(AudioCodec.COPY);
            }
        });
        running(copyOnly);
        assertFalse(bar.getPseudoClassStates().contains(ProgressView.CONVERTING), "only copying: accent bar");
    }

    // ---------------------------------------------------------------- helpers

    /** Marks the job running at the current fake time and shows it, as the queue does. */
    private ConversionJob running(ConversionJob job) throws Exception {
        onFxThread(() -> {
            job.markRunning();
            view.follow(job);
        });
        return job;
    }
}
