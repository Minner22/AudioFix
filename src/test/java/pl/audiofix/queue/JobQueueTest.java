package pl.audiofix.queue;

import javafx.application.Platform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import pl.audiofix.ffmpeg.CommandBuilder;
import pl.audiofix.ffmpeg.FfmpegPaths;
import pl.audiofix.ffmpeg.FfprobeService;
import pl.audiofix.ffmpeg.TestMedia;
import pl.audiofix.model.ConversionJob;
import pl.audiofix.model.ConversionMode;
import pl.audiofix.model.JobStatus;
import pl.audiofix.model.MediaInfo;
import pl.audiofix.model.TrackPlan;
import pl.audiofix.ui.FxTestSupport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.audiofix.ui.FxTestSupport.callOnFxThread;
import static pl.audiofix.ui.FxTestSupport.onFxThread;

/**
 * The queue with the real ffmpeg: jobs run one after another, failures and cancellation
 * end only their own job, shutdown leaves no ffmpeg process behind.
 * Jobs whose output name starts with "slow" run a 30 s real-time ffmpeg command instead, so there is time to cancel them.
 */
@Timeout(value = 90, unit = TimeUnit.SECONDS)
class JobQueueTest {

    @TempDir
    static Path sampleDir;

    @TempDir
    Path dir;

    private static FfmpegPaths paths;
    private static MediaInfo sample;

    private JobQueue queue;

    /** "name:STATUS" in the order the statuses changed, across all jobs. */
    private final List<String> history = new CopyOnWriteArrayList<>();

    @BeforeAll
    static void setUpAll() throws Exception {
        paths = TestMedia.requireFfmpeg();
        sample = new FfprobeService(paths).probe(TestMedia.generateSample(paths.ffmpeg(), sampleDir));
        FxTestSupport.startJavaFx();
    }

    @BeforeEach
    void createQueue() {
        CommandBuilder builder = new CommandBuilder();
        queue = new JobQueue(job -> job.getOutput().getFileName().toString().startsWith("slow")
                ? slowCommand(job.getOutput())
                : builder.build(paths.ffmpeg(), job));
    }

    @AfterEach
    void shutdownQueue() throws Exception {
        onFxThread(queue::shutdown);
    }

    // ---------------------------------------------------------------- order

    @Test
    void jobsRunOneAfterAnother() throws Exception {
        ConversionJob a = job("a");
        ConversionJob b = job("b");
        ConversionJob c = job("c");

        startWith(a, b, c);
        waitUntilIdle();

        assertEquals(List.of("a:RUNNING", "a:DONE", "b:RUNNING", "b:DONE", "c:RUNNING", "c:DONE"), history);
        for (ConversionJob job : List.of(a, b, c)) {
            assertTrue(Files.isRegularFile(job.getOutput()), "no output " + job.getOutput());
        }
    }

    @Test
    void doneJobHasFullProgressAndConvertedAudio() throws Exception {
        ConversionJob job = job("a");

        startWith(job);
        waitUntilIdle();

        assertEquals(1.0, job.getProgress());
        MediaInfo result = new FfprobeService(paths).probe(job.getOutput());
        assertEquals("pcm_s24le", result.streams().get(1).codec());
    }

    @Test
    void logStartsWithCommandAndHasFfmpegOutput() throws Exception {
        ConversionJob job = job("a");

        startWith(job);
        waitUntilIdle();

        List<String> log = callOnFxThread(() -> List.copyOf(job.getLog()));
        assertTrue(log.getFirst().startsWith(paths.ffmpeg().toString()), "first line should be the command: " + log.getFirst());
        assertTrue(log.getFirst().contains("pcm_s24le"), log.getFirst());
        assertTrue(log.stream().anyMatch(line -> line.contains("Stream mapping")), "ffmpeg stderr missing: " + log);
    }

    @Test
    void quickJobUsesQuickCommand() throws Exception {
        ConversionJob job = new ConversionJob(sample, plans(), dir.resolve("quick.mkv"), ConversionMode.QUICK);

        startWith(job);
        waitUntilIdle();

        assertEquals(JobStatus.DONE, job.getStatus());
        assertFalse(callOnFxThread(() -> job.getLog().getFirst()).contains("-map "), "quick command must not map streams");
    }

    @Test
    void runningIsTrueUntilQueueIsEmpty() throws Exception {
        ConversionJob job = job("a");

        assertFalse(callOnFxThread(queue::isRunning));
        boolean runningAfterStart = callOnFxThread(() -> {
            queue.add(job);
            queue.start();
            return queue.isRunning();
        });
        waitUntilIdle();

        assertTrue(runningAfterStart, "running should be set by start() right away");
        assertEquals(JobStatus.DONE, job.getStatus());
    }

    @Test
    void jobAddedWhileRunningIsPickedUp() throws Exception {
        ConversionJob slow = job("slow");
        ConversionJob later = job("later");
        startWith(slow);
        waitUntil(() -> slow.getStatus() == JobStatus.RUNNING, "slow job did not start");

        onFxThread(() -> {
            queue.add(later);
            queue.start();   // already running: must not start a second worker
            queue.cancelCurrent();
        });
        waitUntilIdle();

        assertEquals(List.of("slow:RUNNING", "slow:CANCELLED", "later:RUNNING", "later:DONE"), history);
    }

    @Test
    void queueCanBeStartedAgainAfterItEmptied() throws Exception {
        ConversionJob first = job("first");
        startWith(first);
        waitUntilIdle();

        ConversionJob second = job("second");
        startWith(second);
        waitUntilIdle();

        assertEquals(JobStatus.DONE, second.getStatus());
    }

    // ---------------------------------------------------------------- failures

    @Test
    void failedJobDoesNotStopQueue() throws Exception {
        ConversionJob ok = job("ok");
        ConversionJob broken = new ConversionJob(
                new MediaInfo(dir.resolve("missing.mkv"), 1, sample.streams()), plans(), dir.resolve("broken.mkv"), ConversionMode.PLANNED);
        ConversionJob next = job("next");

        startWith(ok, broken, next);
        waitUntilIdle();

        assertEquals(JobStatus.DONE, ok.getStatus());
        assertEquals(JobStatus.FAILED, broken.getStatus());
        assertTrue(broken.getErrorMessage().contains("ffmpeg"), broken.getErrorMessage());
        assertFalse(Files.exists(broken.getOutput()), "partial output should be deleted");
        assertEquals(JobStatus.DONE, next.getStatus());
    }

    @Test
    void jobWhoseCommandCannotBeBuiltFails() throws Exception {
        onFxThread(queue::shutdown);
        queue = new JobQueue(job -> {
            if (job.getOutput().getFileName().toString().startsWith("bad")) {
                throw new IllegalStateException("Brak ffmpeg");
            }
            return new CommandBuilder().build(paths.ffmpeg(), job);
        });
        ConversionJob bad = job("bad");
        ConversionJob good = job("good");

        startWith(bad, good);
        waitUntilIdle();

        assertEquals(JobStatus.FAILED, bad.getStatus());
        assertEquals("Brak ffmpeg", bad.getErrorMessage());
        assertEquals(JobStatus.DONE, good.getStatus());
    }

    // ---------------------------------------------------------------- cancel

    @Test
    void cancelCurrentStopsRunningJobAndQueueGoesOn() throws Exception {
        ConversionJob slow = job("slow");
        ConversionJob next = job("next");
        startWith(slow, next);
        waitUntil(() -> slow.getStatus() == JobStatus.RUNNING, "slow job did not start");

        onFxThread(queue::cancelCurrent);
        waitUntilIdle();

        assertEquals(JobStatus.CANCELLED, slow.getStatus());
        assertFalse(Files.exists(slow.getOutput()), "partial output should be deleted");
        assertEquals(JobStatus.DONE, next.getStatus());
    }

    @Test
    void cancelWhenNothingRunsDoesNothing() throws Exception {
        ConversionJob job = job("a");
        onFxThread(() -> {
            queue.add(job);
            queue.cancelCurrent();
        });

        assertEquals(JobStatus.PENDING, job.getStatus());
    }

    // ---------------------------------------------------------------- remove

    @Test
    void pendingJobCanBeRemovedAndIsNotConverted() throws Exception {
        ConversionJob slow = job("slow");
        ConversionJob removed = job("removed");
        startWith(slow, removed);
        waitUntil(() -> slow.getStatus() == JobStatus.RUNNING, "slow job did not start");

        assertTrue(callOnFxThread(() -> queue.remove(removed)));
        onFxThread(queue::cancelCurrent);
        waitUntilIdle();

        assertEquals(List.of(slow), queue.getJobs());
        assertEquals(JobStatus.PENDING, removed.getStatus());
        assertFalse(Files.exists(removed.getOutput()));
    }

    @Test
    void runningJobCannotBeRemoved() throws Exception {
        ConversionJob slow = job("slow");
        startWith(slow);
        waitUntil(() -> slow.getStatus() == JobStatus.RUNNING, "slow job did not start");

        assertFalse(callOnFxThread(() -> queue.remove(slow)));
        assertEquals(List.of(slow), callOnFxThread(() -> List.copyOf(queue.getJobs())));
    }

    @Test
    void finishedJobCanBeRemoved() throws Exception {
        ConversionJob job = job("a");
        startWith(job);
        waitUntilIdle();

        assertTrue(callOnFxThread(() -> queue.remove(job)));
        assertEquals(List.of(), queue.getJobs());
    }

    // ---------------------------------------------------------------- shutdown

    @Test
    void shutdownKillsFfmpegAndDeletesPartialOutput() throws Exception {
        ConversionJob slow = job("slow");
        startWith(slow);
        waitUntil(() -> slow.getStatus() == JobStatus.RUNNING, "slow job did not start");
        waitUntilTrue(() -> runningFfmpegCount() == 1, "ffmpeg process not found");

        onFxThread(queue::shutdown);

        assertEquals(0, runningFfmpegCount(), "ffmpeg.exe still running after shutdown");
        assertFalse(Files.exists(slow.getOutput()), "partial output should be deleted");
    }

    @Test
    void queueDoesNotStartAfterShutdown() throws Exception {
        ConversionJob job = job("a");
        onFxThread(queue::shutdown);

        onFxThread(() -> {
            queue.add(job);
            queue.start();
        });

        assertFalse(callOnFxThread(queue::isRunning));
        assertEquals(JobStatus.PENDING, job.getStatus());
    }

    // ---------------------------------------------------------------- threading

    @Test
    void queueMustBeUsedOnFxThread() {
        ConversionJob job = job("a");

        assertThrows(IllegalStateException.class, () -> queue.add(job));
        assertThrows(IllegalStateException.class, () -> queue.start());
    }

    @Test
    void statusChangesArriveOnFxThread() throws Exception {
        ConversionJob job = job("a");
        List<Boolean> onFx = new CopyOnWriteArrayList<>();
        onFxThread(() -> job.statusProperty().addListener(
                (observable, previous, status) -> onFx.add(Platform.isFxApplicationThread())));

        startWith(job);
        waitUntilIdle();

        assertEquals(List.of(true, true), onFx);
    }

    // ---------------------------------------------------------------- helpers

    private ConversionJob job(String name) {
        ConversionJob job = new ConversionJob(sample, plans(), dir.resolve(name + ".mkv"), ConversionMode.PLANNED);
        job.statusProperty().addListener((observable, previous, status) -> history.add(name + ":" + status));
        return job;
    }

    private static List<TrackPlan> plans() {
        return sample.streams().stream().map(TrackPlan::defaultsFor).toList();
    }

    private void startWith(ConversionJob... jobs) throws Exception {
        onFxThread(() -> {
            for (ConversionJob job : jobs) {
                queue.add(job);
            }
            queue.start();
        });
    }

    /** 30 s of generated audio at real-time speed: long enough to cancel. */
    private static List<String> slowCommand(Path output) {
        return List.of(paths.ffmpeg().toString(), "-hide_banner", "-y",
                "-re", "-f", "lavfi", "-i", "sine=frequency=440:duration=30",
                "-c:a", "pcm_s16le", "-progress", "pipe:1", "-nostats", output.toString());
    }

    private static long runningFfmpegCount() {
        return ProcessHandle.current().descendants()
                .filter(ProcessHandle::isAlive)
                .filter(p -> p.info().command().map(c -> c.toLowerCase().endsWith("ffmpeg.exe")).orElse(false))
                .count();
    }

    private void waitUntilIdle() throws Exception {
        waitUntil(() -> !queue.isRunning()
                && queue.getJobs().stream().noneMatch(j -> j.getStatus() == JobStatus.RUNNING), "queue did not finish");
    }

    /** Polls the condition on the JavaFX Application Thread. */
    private static void waitUntil(BooleanSupplier condition, String message) throws Exception {
        waitUntilTrue(() -> callOnFxThread(condition::getAsBoolean), message);
    }

    private interface Check {
        boolean holds() throws Exception;
    }

    private static void waitUntilTrue(Check condition, String message) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (System.nanoTime() < deadline) {
            if (condition.holds()) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError(message);
    }
}
