package pl.audiofix.model;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversionJobTest {

    private static final Path INPUT = Path.of("D:\\Filmy\\Film.mkv");
    private static final Path OUTPUT = Path.of("D:\\Filmy\\Film_fixed.mkv");

    // ---------------------------------------------------------------- new job

    @Test
    void newJobIsPendingAndEditable() {
        ConversionJob job = job();

        assertEquals(JobStatus.PENDING, job.getStatus());
        assertTrue(job.isEditable());
        assertEquals(0.0, job.getProgress());
        assertNull(job.getErrorMessage());
        assertEquals(List.of(), job.getLog());
    }

    @Test
    void inputComesFromMediaInfo() {
        ConversionJob job = job();

        assertEquals(INPUT, job.getInput());
        assertEquals(OUTPUT, job.getOutput());
        assertEquals(ConversionMode.PLANNED, job.getMode());
    }

    @Test
    void plansAreCopiedSoLaterListChangesDoNotLeakIn() {
        List<TrackPlan> plans = new ArrayList<>(plans());
        ConversionJob job = new ConversionJob(media(), plans, OUTPUT, ConversionMode.PLANNED);

        plans.clear();

        assertEquals(2, job.getPlans().size());
        assertThrows(UnsupportedOperationException.class, () -> job.getPlans().clear());
    }

    @Test
    void logCannotBeChangedFromOutside() {
        ConversionJob job = job();

        assertThrows(UnsupportedOperationException.class, () -> job.getLog().add("x"));
    }

    @Test
    void requiredArgumentsAreChecked() {
        assertThrows(NullPointerException.class, () -> new ConversionJob(null, plans(), OUTPUT, ConversionMode.PLANNED));
        assertThrows(NullPointerException.class, () -> new ConversionJob(media(), plans(), null, ConversionMode.PLANNED));
        assertThrows(NullPointerException.class, () -> new ConversionJob(media(), plans(), OUTPUT, null));
    }

    // ---------------------------------------------------------------- happy path

    @Test
    void successfulJobGoesPendingRunningDone() {
        ConversionJob job = job();
        List<JobStatus> seen = new ArrayList<>();
        job.statusProperty().addListener((observable, previous, status) -> seen.add(status));

        job.markRunning();
        job.setProgress(0.5);
        job.markDone();

        assertEquals(List.of(JobStatus.RUNNING, JobStatus.DONE), seen);
        assertEquals(1.0, job.getProgress());
    }

    @Test
    void progressIsClampedToZeroOne() {
        ConversionJob job = job();
        job.markRunning();

        job.setProgress(1.7);
        assertEquals(1.0, job.getProgress());
        job.setProgress(-0.2);
        assertEquals(0.0, job.getProgress());
    }

    @Test
    void logKeepsLinesInOrder() {
        ConversionJob job = job();

        job.appendLog("ffmpeg -i in.mkv out.mkv");
        job.appendLog("Stream mapping:");

        assertEquals(List.of("ffmpeg -i in.mkv out.mkv", "Stream mapping:"), job.getLog());
    }

    // ---------------------------------------------------------------- failure and cancellation

    @Test
    void failedJobKeepsErrorMessageAndResetsProgress() {
        ConversionJob job = job();
        job.markRunning();
        job.setProgress(0.4);

        job.markFailed("ffmpeg zakończył się błędem (kod 1)");

        assertEquals(JobStatus.FAILED, job.getStatus());
        assertEquals("ffmpeg zakończył się błędem (kod 1)", job.getErrorMessage());
        assertEquals(0.0, job.getProgress());
    }

    @Test
    void runningJobCanBeCancelled() {
        ConversionJob job = job();
        job.markRunning();
        job.setProgress(0.4);

        job.markCancelled();

        assertEquals(JobStatus.CANCELLED, job.getStatus());
        assertEquals(0.0, job.getProgress());
    }

    @Test
    void pendingJobCanBeCancelledWithoutRunning() {
        ConversionJob job = job();

        job.markCancelled();

        assertEquals(JobStatus.CANCELLED, job.getStatus());
    }

    // ---------------------------------------------------------------- illegal transitions

    @Test
    void jobCannotBeDoneWithoutRunning() {
        assertThrows(IllegalStateException.class, () -> job().markDone());
    }

    @Test
    void jobCannotFailWithoutRunning() {
        ConversionJob job = job();

        assertThrows(IllegalStateException.class, () -> job.markFailed("x"));
        assertNull(job.getErrorMessage(), "a rejected change must not leave a message behind");
    }

    @Test
    void jobRunsOnlyOnce() {
        ConversionJob job = job();
        job.markRunning();
        job.markDone();

        assertThrows(IllegalStateException.class, job::markRunning);
    }

    @Test
    void finishedJobCannotBeCancelled() {
        ConversionJob job = job();
        job.markRunning();
        job.markDone();

        assertThrows(IllegalStateException.class, job::markCancelled);
        assertEquals(JobStatus.DONE, job.getStatus());
    }

    // ---------------------------------------------------------------- editing

    @Test
    void outputCanBeChangedWhilePending() {
        ConversionJob job = job();
        Path other = Path.of("E:\\Wynik.mkv");

        job.setOutput(other);

        assertEquals(other, job.getOutput());
    }

    @Test
    void outputCannotBeChangedOnceStarted() {
        ConversionJob job = job();
        job.markRunning();

        assertFalse(job.isEditable());
        assertThrows(IllegalStateException.class, () -> job.setOutput(Path.of("E:\\Wynik.mkv")));
        assertEquals(OUTPUT, job.getOutput());
    }

    @Test
    void modeCanBeChangedWhilePending() {
        ConversionJob job = job();

        job.setMode(ConversionMode.QUICK);

        assertEquals(ConversionMode.QUICK, job.getMode());
        assertEquals(ConversionMode.QUICK, job.modeProperty().get());
    }

    @Test
    void modeCannotBeChangedOnceStarted() {
        ConversionJob job = job();
        job.markRunning();

        assertThrows(IllegalStateException.class, () -> job.setMode(ConversionMode.QUICK));
        assertEquals(ConversionMode.PLANNED, job.getMode());
    }

    @Test
    void finishedJobIsNotEditable() {
        ConversionJob job = job();
        job.markCancelled();

        assertFalse(job.isEditable());
    }

    @Test
    void statusKnowsWhichStatesAreFinal() {
        assertFalse(JobStatus.PENDING.isFinished());
        assertFalse(JobStatus.RUNNING.isFinished());
        assertTrue(JobStatus.DONE.isFinished());
        assertTrue(JobStatus.FAILED.isFinished());
        assertTrue(JobStatus.CANCELLED.isFinished());
    }

    // ---------------------------------------------------------------- helpers

    private static ConversionJob job() {
        return new ConversionJob(media(), plans(), OUTPUT, ConversionMode.PLANNED);
    }

    private static MediaInfo media() {
        return new MediaInfo(INPUT, 60, List.of());
    }

    private static List<TrackPlan> plans() {
        return List.of(
                TrackPlan.defaultsFor(new StreamInfo(0, StreamType.VIDEO, "hevc", null, 0, null, "und", null, true)),
                TrackPlan.defaultsFor(new StreamInfo(1, StreamType.AUDIO, "dts", null, 6, null, "eng", null, true)));
    }
}
