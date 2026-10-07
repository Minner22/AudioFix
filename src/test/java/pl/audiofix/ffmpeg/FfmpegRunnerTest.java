package pl.audiofix.ffmpeg;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import pl.audiofix.model.MediaInfo;
import pl.audiofix.model.TrackPlan;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests with the real ffmpeg. Skipped when ffmpeg cannot be found.
 */
class FfmpegRunnerTest {

    @TempDir
    static Path dir;

    private static FfmpegPaths paths;
    private static MediaInfo sample;

    @BeforeAll
    static void setUp() throws Exception {
        paths = TestMedia.requireFfmpeg();
        sample = new FfprobeService(paths).probe(TestMedia.generateSample(paths.ffmpeg(), dir));
    }

    // ---------------------------------------------------------------- success

    @Test
    void convertsFileAndReportsProgressUpToOne() {
        Path output = dir.resolve("converted ąę.mkv");
        RecordingListener listener = new RecordingListener();

        new FfmpegRunner().run(convertCommand(output), output, sample.durationSec(), listener);

        assertTrue(Files.isRegularFile(output));
        assertFalse(listener.progress.isEmpty(), "no progress reported");
        assertEquals(1.0, listener.progress.getLast(), "last progress should be 1.0");
        for (int i = 1; i < listener.progress.size(); i++) {
            assertTrue(listener.progress.get(i) >= listener.progress.get(i - 1), "progress went backwards: " + listener.progress);
        }
        listener.progress.forEach(p -> assertTrue(p >= 0.0 && p <= 1.0, "progress out of range: " + p));
    }

    @Test
    void convertedFileHasPcmAudio() {
        Path output = dir.resolve("pcm.mkv");

        new FfmpegRunner().run(convertCommand(output), output, sample.durationSec(), new RecordingListener());

        MediaInfo result = new FfprobeService(paths).probe(output);
        assertEquals("pcm_s24le", result.audioStreams().getFirst().codec());
    }

    @Test
    void ffmpegStderrIsPassedAsLog() {
        Path output = dir.resolve("log.mkv");
        RecordingListener listener = new RecordingListener();

        new FfmpegRunner().run(convertCommand(output), output, sample.durationSec(), listener);

        assertTrue(listener.logs.stream().anyMatch(line -> line.contains("Stream mapping")),
                "expected ffmpeg stderr in logs, got: " + listener.logs);
    }

    @Test
    void progressIsNotReportedOnStderrLog() {
        Path output = dir.resolve("nolog.mkv");
        RecordingListener listener = new RecordingListener();

        new FfmpegRunner().run(convertCommand(output), output, sample.durationSec(), listener);

        assertTrue(listener.logs.stream().noneMatch(line -> line.startsWith("out_time_us=")),
                "progress lines (stdout) must not end up in the log (stderr)");
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void exceptionInListenerDoesNotStopFurtherUpdates() {
        // a bug in UI code for one update must not freeze the progress bar for the rest of the conversion
        Path output = dir.resolve("listener.mkv");
        RecordingListener recording = new RecordingListener();
        FfmpegListener failingOnce = new FfmpegListener() {
            private boolean progressFailed;
            private boolean logFailed;

            @Override
            public void onProgress(double fraction) {
                if (!progressFailed) {
                    progressFailed = true;
                    throw new IllegalStateException("bug in listener");
                }
                recording.onProgress(fraction);
            }

            @Override
            public void onLog(String line) {
                if (!logFailed) {
                    logFailed = true;
                    throw new IllegalStateException("bug in listener");
                }
                recording.onLog(line);
            }
        };

        new FfmpegRunner().run(mediumCommand(output), output, MEDIUM_DURATION_SEC, failingOnce);

        assertFalse(recording.progress.isEmpty(), "no progress after the first listener failure");
        assertEquals(1.0, recording.progress.getLast());
        assertFalse(recording.logs.isEmpty(), "no log lines after the first listener failure");
    }

    // ---------------------------------------------------------------- failure

    @Test
    void failedConversionThrowsWithFfmpegMessageAndRemovesOutput() throws Exception {
        Path fake = Files.writeString(dir.resolve("fake.mkv"), "this is not a video");
        Path output = dir.resolve("fake_fixed.mkv");
        List<String> command = new CommandBuilder().build(paths.ffmpeg(), fake, output, List.of());

        var ex = assertThrows(FfmpegException.class,
                () -> new FfmpegRunner().run(command, output, 0, new RecordingListener()));

        assertTrue(ex.getMessage().contains("Invalid data"), "message should contain ffmpeg stderr, was: " + ex.getMessage());
        assertFalse(Files.exists(output));
    }

    @Test
    void missingExecutableThrowsFfmpegException() {
        Path output = dir.resolve("never.mkv");
        List<String> command = List.of(dir.resolve("no-such-ffmpeg.exe").toString(), "-version");

        assertThrows(FfmpegException.class,
                () -> new FfmpegRunner().run(command, output, 0, new RecordingListener()));
    }

    // ---------------------------------------------------------------- cancel

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void cancelStopsFfmpegAndRemovesPartialOutput() throws Exception {
        Path output = dir.resolve("cancelled.mkv");
        FfmpegRunner runner = new FfmpegRunner();
        CountDownLatch started = new CountDownLatch(1);
        FfmpegListener listener = new FfmpegListener() {
            @Override
            public void onProgress(double fraction) {
                started.countDown();
            }
        };

        CompletableFuture<Void> conversion = CompletableFuture.runAsync(
                () -> runner.run(longCommand(output), output, LONG_DURATION_SEC, listener));

        assertTrue(started.await(20, TimeUnit.SECONDS), "conversion did not start");
        runner.cancel();

        var ex = assertThrows(ExecutionException.class, () -> conversion.get(10, TimeUnit.SECONDS));
        assertInstanceOf(CancellationException.class, ex.getCause());
        assertFalse(Files.exists(output), "partial output should be deleted");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void interruptingWorkerThreadCancelsConversion() throws Exception {
        Path output = dir.resolve("interrupted.mkv");
        FfmpegRunner runner = new FfmpegRunner();
        CountDownLatch started = new CountDownLatch(1);
        List<Throwable> thrown = Collections.synchronizedList(new ArrayList<>());
        FfmpegListener listener = new FfmpegListener() {
            @Override
            public void onProgress(double fraction) {
                started.countDown();
            }
        };

        Thread worker = new Thread(() -> {
            try {
                runner.run(longCommand(output), output, LONG_DURATION_SEC, listener);
            } catch (Throwable t) {
                thrown.add(t);
            }
        });
        worker.start();

        assertTrue(started.await(20, TimeUnit.SECONDS), "conversion did not start");
        worker.interrupt();
        worker.join(10_000);

        assertFalse(worker.isAlive(), "runner did not react to interrupt");
        assertEquals(1, thrown.size());
        assertInstanceOf(CancellationException.class, thrown.getFirst());
        assertFalse(Files.exists(output), "partial output should be deleted");
    }

    // ---------------------------------------------------------------- helpers

    private static final int LONG_DURATION_SEC = 600;

    private static List<String> convertCommand(Path output) {
        List<TrackPlan> plans = sample.streams().stream().map(TrackPlan::defaultsFor).toList();
        return new CommandBuilder().build(paths.ffmpeg(), sample.path(), output, plans);
    }

    /** Encodes 10 minutes of generated video - takes long enough to be cancelled in the middle. */
    private static List<String> longCommand(Path output) {
        return List.of(paths.ffmpeg().toString(), "-hide_banner", "-y",
                "-f", "lavfi", "-i", "testsrc=duration=" + LONG_DURATION_SEC + ":size=1280x720:rate=30",
                "-c:v", "mpeg4", "-q:v", "2",
                "-progress", "pipe:1", "-nostats",
                output.toString());
    }

    private static final int MEDIUM_DURATION_SEC = 120;

    /** Encodes 2 minutes of generated video - a few seconds of work, several progress reports. */
    private static List<String> mediumCommand(Path output) {
        return List.of(paths.ffmpeg().toString(), "-hide_banner", "-y",
                "-f", "lavfi", "-i", "testsrc=duration=" + MEDIUM_DURATION_SEC + ":size=1280x720:rate=30",
                "-c:v", "mpeg4",
                "-progress", "pipe:1", "-nostats",
                output.toString());
    }

    private static final class RecordingListener implements FfmpegListener {

        final List<Double> progress = Collections.synchronizedList(new ArrayList<>());
        final List<String> logs = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void onProgress(double fraction) {
            progress.add(fraction);
        }

        @Override
        public void onLog(String line) {
            logs.add(line);
        }
    }
}
