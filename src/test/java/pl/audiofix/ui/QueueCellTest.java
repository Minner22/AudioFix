package pl.audiofix.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pl.audiofix.model.ConversionJob;
import pl.audiofix.model.ConversionMode;
import pl.audiofix.model.JobStatus;
import pl.audiofix.model.MediaInfo;
import pl.audiofix.ui.theme.Theme;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.audiofix.ui.FxTestSupport.callOnFxThread;
import static pl.audiofix.ui.FxTestSupport.onFxThread;

/** One item of the queue list: name, size and container, live status badge and the small progress bar. */
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

    // ---------------------------------------------------------------- size and container

    @ParameterizedTest(name = "{0} B -> \"{1}\"")
    @CsvSource(delimiter = '|', textBlock = """
            0              | 0 B
            1023           | 1023 B
            1024           | 1,0 KB
            1536           | 1,5 KB
            5242880        | 5,0 MB
            1503238553     | 1,4 GB
            34789235712    | 32,4 GB
            2199023255552  | 2,0 TB
            """)
    void sizeLikeWindowsExplorer(long bytes, String expected) {
        assertEquals(expected, QueueCell.sizeText(bytes));
    }

    @ParameterizedTest(name = "{0} -> \"{1}\"")
    @CsvSource(delimiter = '|', textBlock = """
            Film.mkv              | MKV
            Film.2011.REMUX.mp4   | MP4
            Film.M2TS             | M2TS
            Film                  | ''
            .mkv                  | ''
            """)
    void containerIsExtensionInCapitals(String name, String expected) {
        assertEquals(expected, QueueCell.containerText(Path.of(name)));
    }

    @Test
    void detailsHaveSizeAndContainer() {
        MediaInfo media = new MediaInfo(Path.of("D:\\Filmy\\Drive.mkv"), 60, List.of(), 34_789_235_712L);

        assertEquals("32,4 GB • MKV", QueueCell.detailsText(media));
    }

    @Test
    void unknownSizeShowsOnlyContainer() {
        MediaInfo media = new MediaInfo(Path.of("D:\\Filmy\\Drive.mkv"), 60, List.of());

        assertEquals("MKV", QueueCell.detailsText(media));
    }

    // ---------------------------------------------------------------- shown job

    @Test
    void showsDetailsUnderName() throws Exception {
        ConversionJob big = job("Flow.2024.mkv", 19_434_825_318L);
        onFxThread(() -> cell.updateItem(big, false));

        assertEquals("18,1 GB • MKV", label(".queue-details").getText());
    }

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

    // ---------------------------------------------------------------- look (light theme)

    private static final Color ACCENT = Color.web("#0284c7");
    private static final Color SELECTION = Color.web("#e0f2fe");
    private static final Color GRIDLINE = Color.web("#f1f5f9");

    private static final String LONG_NAME = "Drive.2011.UHD.BluRay.2160p.DTS-HD.MA.7.1.HEVC.REMUX-FraMeSToR.mkv";

    @Test
    void longNameIsShortenedInTheMiddleAndTooltipHasItWhole() throws Exception {
        ConversionJob longJob = job(LONG_NAME);
        ListView<ConversionJob> list = styledList(longJob);

        QueueCell longCell = cellOf(list, longJob);
        String shown = shownText(longCell.lookup(".queue-name"));

        assertTrue(shown.contains("…"), "name not shortened: " + shown);
        assertTrue(shown.startsWith("Drive"), "start of the name must stay: " + shown);
        assertTrue(shown.endsWith(".mkv"), "end of the name must stay: " + shown);
        assertTrue(longCell.getTooltip().getText().contains(LONG_NAME));
    }

    @Test
    void longNameDoesNotMakeListScrollSideways() throws Exception {
        ListView<ConversionJob> list = styledList(job(LONG_NAME));

        boolean sideways = callOnFxThread(() -> list.lookupAll(".scroll-bar").stream()
                .map(ScrollBar.class::cast)
                .anyMatch(bar -> bar.getOrientation() == Orientation.HORIZONTAL && bar.isVisible()));
        assertFalse(sideways, "horizontal scroll bar shown for a long name");
    }

    @Test
    void badgeIsNeverShortenedInNarrowList() throws Exception {
        ConversionJob longJob = job(LONG_NAME, 34_789_235_712L);
        onFxThread(() -> {
            longJob.setMode(ConversionMode.QUICK);
            longJob.markRunning();
            longJob.setProgress(0.42);
        });
        ListView<ConversionJob> list = styledList(150, longJob);

        assertEquals("W trakcie 42% · szybka", shownText(cellOf(list, longJob).lookup(".queue-status")));
    }

    @ParameterizedTest(name = "{0}: fill {2}, text {3}")
    @CsvSource(delimiter = '|', textBlock = """
            PENDING   | #e2e8f0 | #e2e8f0 | #475569
            RUNNING   | #0284c7 | #0284c7 | #ffffff
            DONE      | #bbf7d0 | #f0fdf4 | #15803d
            FAILED    | #fca5a5 | #fef2f2 | #b91c1c
            CANCELLED | #cbd5e1 | #ffffff | #64748b
            """)
    void badgeColorsFollowStatus(JobStatus status, String border, String fill, String text) throws Exception {
        onFxThread(() -> moveTo(job, status));
        ListView<ConversionJob> list = styledList(job);

        Label badge = (Label) cellOf(list, job).lookup(".queue-status");
        assertEquals(Color.web(border), badge.getBackground().getFills().get(0).getFill(), "border layer");
        assertEquals(Color.web(fill), badge.getBackground().getFills().get(1).getFill(), "fill layer");
        assertEquals(Color.web(text), badge.getTextFill());
    }

    @Test
    void itemsAreSeparatedByHairline() throws Exception {
        ListView<ConversionJob> list = styledList(job);

        List<BackgroundFill> fills = cellOf(list, job).getBackground().getFills();
        assertEquals(GRIDLINE, fills.getFirst().getFill(), "separator layer");
        assertEquals(1, fills.getLast().getInsets().getBottom(), "1px of the separator shows under the item");
    }

    @Test
    void selectedItemHasAccentEdgeAndBoldName() throws Exception {
        ConversionJob other = job("Flow.2024.mkv");
        ListView<ConversionJob> list = styledList(job, other);
        onFxThread(() -> {
            list.getSelectionModel().select(job);
            FxTestSupport.layout(list);
        });

        List<BackgroundFill> fills = cellOf(list, job).getBackground().getFills();
        assertEquals(3, fills.size(), fills.toString());
        assertEquals(ACCENT, fills.get(1).getFill(), "left edge in accent");
        assertEquals(SELECTION, fills.get(2).getFill(), "selection background");
        assertEquals(3, fills.get(2).getInsets().getLeft(), "3px of the accent shows on the left");
        assertEquals(0, fills.get(2).getInsets().getTop(), "edge only on the left");

        assertTrue(nameOf(list, job).getFont().getStyle().contains("Bold"), nameOf(list, job).getFont().getStyle());
        assertFalse(nameOf(list, other).getFont().getStyle().contains("Bold"), "only the selected name is bold");
    }

    @Test
    void selectingDoesNotMoveContent() throws Exception {
        ListView<ConversionJob> list = styledList(job);
        Label name = nameOf(list, job);
        double before = callOnFxThread(() -> name.localToScene(0, 0).getX());

        onFxThread(() -> {
            list.getSelectionModel().select(job);
            FxTestSupport.layout(list);
        });
        Label selectedName = nameOf(list, job);
        double after = callOnFxThread(() -> selectedName.localToScene(0, 0).getX());

        assertEquals(before, after, "the accent edge must not push the content");
    }

    // ---------------------------------------------------------------- helpers

    private static ListView<ConversionJob> styledList(ConversionJob... jobs) throws Exception {
        return styledList(240, jobs);
    }

    private static ListView<ConversionJob> styledList(double width, ConversionJob... jobs) throws Exception {
        return callOnFxThread(() -> {
            ListView<ConversionJob> list = new ListView<>(FXCollections.observableArrayList(jobs));
            list.setCellFactory(view -> new QueueCell());
            Theme.apply(new Scene(list, width, 300));
            FxTestSupport.layout(list);
            return list;
        });
    }

    private static QueueCell cellOf(ListView<ConversionJob> list, ConversionJob job) throws Exception {
        return callOnFxThread(() -> list.lookupAll(".queue-cell").stream()
                .map(QueueCell.class::cast)
                .filter(cell -> cell.getItem() == job)
                .findFirst()
                .orElseThrow());
    }

    private static Label nameOf(ListView<ConversionJob> list, ConversionJob job) throws Exception {
        return (Label) cellOf(list, job).lookup(".queue-name");
    }

    /** Text the label really shows, after shortening with the ellipsis. */
    private static String shownText(Node label) throws Exception {
        return callOnFxThread(() -> ((Text) label.lookup(".text")).getText());
    }

    private static void moveTo(ConversionJob job, JobStatus status) {
        switch (status) {
            case PENDING -> { }
            case RUNNING -> job.markRunning();
            case DONE -> {
                job.markRunning();
                job.markDone();
            }
            case FAILED -> {
                job.markRunning();
                job.markFailed("błąd");
            }
            case CANCELLED -> job.markCancelled();
        }
    }

    private Label label(String selector) {
        assertNotNull(cell.getGraphic(), "cell shows nothing");
        return (Label) cell.getGraphic().lookup(selector);
    }

    private ProgressBar progressBar() {
        return (ProgressBar) cell.getGraphic().lookup(".queue-progress");
    }

    private static ConversionJob job(String name) {
        return job(name, 0);
    }

    private static ConversionJob job(String name, long sizeBytes) {
        Path input = Path.of("D:\\Filmy").resolve(name);
        return new ConversionJob(new MediaInfo(input, 60, List.of(), sizeBytes), List.of(),
                OutputPathResolver.defaultOutput(input), ConversionMode.PLANNED);
    }
}
