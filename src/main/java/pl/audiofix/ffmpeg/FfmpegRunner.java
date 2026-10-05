package pl.audiofix.ffmpeg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FfmpegRunner {

    private static final Logger log = LoggerFactory.getLogger(FfmpegRunner.class);
    private static final String OUT_TIME_PREFIX = "out_time_us=";
    private static final int LOG_TAIL_LINES = 20;
    private static final long EXIT_WAIT_SECONDS = 10;

    private volatile Process process;
    private volatile boolean cancelled;

    public void run(List<String> command, Path output, double durationSec, FfmpegListener listener) {

        log.info("Running {}", command);
        LogTail logTail = new LogTail(LOG_TAIL_LINES);
        boolean succeeded = false;

        try {
            Process p = start(command);
            Thread progressReader = startReader(p.getInputStream(),
                    line -> progressFrom(line, durationSec).ifPresent(listener::onProgress));
            Thread logReader = startReader(p.getErrorStream(), logTail.andThen(listener::onLog));

            int exitCode = p.waitFor();
            progressReader.join();
            logReader.join();

            checkResult(exitCode, logTail);
            succeeded = true;
            log.info("Finished {}", output);
        } catch (IOException e) {
            throw new FfmpegException("Nie można uruchomić ffmpeg: " + command.getFirst(), e);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            throw new CancellationException("Przerwano konwersję");
        } finally {
            if (!succeeded) {
                destroyAndWait();
                deleteQuietly(output);
            }
        }
    }

    public void cancel() {

        cancelled = true;
        Process current = process;
        if (current != null) {
            current.destroy();
        }
    }

    static OptionalDouble progressFrom(String line, double durationSec) {

        if (line.equals("progress=end")) {
            return OptionalDouble.of(1.0);
        }

        if (!line.startsWith(OUT_TIME_PREFIX) || durationSec <= 0) {
            return OptionalDouble.empty();
        }

        String value = line.substring(OUT_TIME_PREFIX.length());
        try {
            long outTimeUs = Long.parseLong(value);
            double fraction = outTimeUs / (durationSec * 1_000_000);

            return OptionalDouble.of(Math.clamp(fraction, 0.0, 1.0));
        } catch (NumberFormatException _) {
            return OptionalDouble.empty();
        }
    }

    private Process start(List<String> command) throws IOException {

        Process p = new ProcessBuilder(command).start();
        process = p;
        p.getOutputStream().close();

        if (cancelled) {
            p.destroy();
        }

        return p;
    }

    private void checkResult(int exitCode, LogTail logTail) {

        if (cancelled) {
            throw new CancellationException("Anulowano konwersję");
        }

        if (exitCode != 0) {
            throw new FfmpegException("ffmpeg zakończył się błędem (kod " + exitCode + "):\n" + logTail);
        }
    }

    private void destroyAndWait() {

        Process current = process;
        if (current == null) {
            return;
        }

        current.destroy();
        boolean interrupted = Thread.interrupted();
        try {
            current.waitFor(EXIT_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException _) {
            interrupted = true;
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static Thread startReader(InputStream in, Consumer<String> consumer) {

        return Thread.ofVirtual().start(() -> readLines(in, consumer));
    }

    private static void readLines(InputStream in, Consumer<String> consumer) {

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                acceptSafely(consumer, line);
            }
        } catch (IOException _) {
            // stream closed because process was killed; normal when canceled
        }
    }

    private static void acceptSafely(Consumer<String> consumer, String line) {

        try {
            consumer.accept(line);
        } catch (RuntimeException e) {
            log.warn("Listener failed for line: {}", line, e);
        }
    }

    private static void deleteQuietly(Path file) {

        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Cannot delete {}", file, e);
        }
    }

    private static final class LogTail implements Consumer<String> {

        private final Deque<String> lines = new ArrayDeque<>();
        private final int maxLines;

        LogTail(int maxLines) {

            this.maxLines = maxLines;
        }

        @Override
        public void accept(String line) {

            lines.addLast(line);
            if (lines.size() > maxLines) {
                lines.removeFirst();
            }
        }

        @Override
        public String toString() {

            return String.join("\n", lines);
        }
    }
}
