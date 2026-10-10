package pl.audiofix.ui;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pl.audiofix.model.ConversionJob;
import pl.audiofix.model.ConversionMode;
import pl.audiofix.model.JobStatus;
import pl.audiofix.model.MediaInfo;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.audiofix.ui.FxTestSupport.onFxThread;

/** One row of the queue list: file name, live status and the small progress bar. */
class QueueCellTest {

    private ConversionJob job;
    private QueueCell cell;

    @BeforeAll
    static void startJavaFx() {
        FxTestSupport.startJavaFx();
    }

    @BeforeEach
    void createCell() throws Exception {
        job = job("Drive.2011.REMUX.mkv");
        onFxThread(() -> {
            cell = new QueueCell();
            ListView<ConversionJob> list = new ListView<>();
            new Scene(list);
            cell.updateListView(list);
            cell.updateItem(job, false);
        });
    }

    // ---------------------------------------------------------------- status text

    @ParameterizedTest(name = "{0} {1} -> \"{2}\"")
    @CsvSource(delimiter = '|', textBlock = """
            PENDING   | 0     | Oczekuje
            RUNNING   | 0     | W trakcie 0%
            RUNNING   | 0.424 | W trakcie 42%
            RUNNING   | 0.996 | W trakcie 100%
            DONE      | 1     | Gotowe
            FAILED    | 0     | Błąd
            CANCELLED | 0     | Anulowano
            """)
    void statusTextForEveryStatus(JobStatus status, double progress, String expected) {
        assertEquals(expected, QueueCell.statusText(status, progress, ConversionMode.PLANNED));
    }

    @Test
    void quickJobsAreMarked() {
        assertEquals("Oczekuje · szybka", QueueCell.statusText(JobStatus.PENDING, 0, ConversionMode.QUICK));
    }

    @Test
    void everyStatusHasItsOwnStyleClass() {
        assertEquals("status-running", QueueCell.statusClass(JobStatus.RUNNING));
        assertEquals(JobStatus.values().length, QueueCell.STATUS_CLASSES.stream().distinct().count());
    }

    // ---------------------------------------------------------------- shown job

    @Test
    void showsFileNameAndStatus() {
        assertEquals("Drive.2011.REMUX.mkv", label(".queue-name").getText());
        assertEquals("Oczekuje", label(".queue-status").getText());
        assertTrue(label(".queue-status").getStyleClass().contains("status-pending"));
    }

    @Test
    void tooltipHasFullPath() {
        assertEquals(job.getInput().toString(), cell.getTooltip().getText());
    }

    @Test
    void progressBarOnlyWhileRunning() throws Exception {
        assertFalse(progressBar().isVisible(), "pending");

        onFxThread(() -> {
            job.markRunning();
            job.setProgress(0.5);
        });
        assertTrue(progressBar().isVisible(), "running");
        assertEquals(0.5, progressBar().getProgress());

        onFxThread(job::markDone);
        assertFalse(progressBar().isVisible(), "done");
        assertFalse(progressBar().isManaged(), "a hidden bar must not take space");
    }

    @Test
    void statusFollowsJobLive() throws Exception {
        onFxThread(job::markRunning);
        onFxThread(() -> job.setProgress(0.42));

        assertEquals("W trakcie 42%", label(".queue-status").getText());
        assertTrue(label(".queue-status").getStyleClass().contains("status-running"));
        assertFalse(label(".queue-status").getStyleClass().contains("status-pending"), "old status class must go");
    }

    @Test
    void failedJobShowsErrorInTooltip() throws Exception {
        onFxThread(() -> {
            job.markRunning();
            job.markFailed("ffmpeg zakończył się błędem (kod 1)");
        });

        assertEquals("Błąd", label(".queue-status").getText());
        assertTrue(cell.getTooltip().getText().contains("ffmpeg zakończył się błędem (kod 1)"), cell.getTooltip().getText());
    }

    @Test
    void modeChangeIsShown() throws Exception {
        onFxThread(() -> job.setMode(ConversionMode.QUICK));

        assertEquals("Oczekuje · szybka", label(".queue-status").getText());
    }

    // ---------------------------------------------------------------- reuse

    @Test
    void reusedCellNoLongerFollowsPreviousJob() throws Exception {
        ConversionJob other = job("Other.mkv");

        onFxThread(() -> cell.updateItem(other, false));
        onFxThread(() -> {
            job.markRunning();
            job.setProgress(0.7);
        });

        assertEquals("Other.mkv", label(".queue-name").getText());
        assertEquals("Oczekuje", label(".queue-status").getText());
        assertEquals(0.0, progressBar().getProgress());
    }

    @Test
    void emptyCellShowsNothing() throws Exception {
        onFxThread(() -> cell.updateItem(null, true));

        assertNull(cell.getGraphic());
        assertNull(cell.getTooltip());
    }

    // ---------------------------------------------------------------- helpers

    private Label label(String selector) {
        assertNotNull(cell.getGraphic(), "cell shows nothing");
        return (Label) cell.getGraphic().lookup(selector);
    }

    private ProgressBar progressBar() {
        return (ProgressBar) cell.getGraphic().lookup(".queue-progress");
    }

    private static ConversionJob job(String name) {
        Path input = Path.of("D:\\Filmy").resolve(name);
        return new ConversionJob(new MediaInfo(input, 60, List.of()), List.of(),
                OutputPathResolver.defaultOutput(input), ConversionMode.PLANNED);
    }
}
