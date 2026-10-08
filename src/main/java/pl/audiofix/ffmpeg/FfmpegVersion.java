package pl.audiofix.ffmpeg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class FfmpegVersion {

    private static final Logger log = LoggerFactory.getLogger(FfmpegVersion.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private FfmpegVersion() {

    }

    public static Optional<String> firstLine(Path ffmpeg) {

        return firstLine(ffmpeg, TIMEOUT);
    }

    static Optional<String> firstLine(Path ffmpeg, Duration timeout) {

        try {
            Process process = new ProcessBuilder(ffmpeg.toString(), "-version")
                    .redirectErrorStream(true)
                    .start();
            process.getOutputStream().close();

            // read in the background, so that waitFor() below can time out even if ffmpeg never closes its output
            CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> readAll(process.getInputStream()));

            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                log.warn("ffmpeg -version timed out: {}", ffmpeg);
                return Optional.empty();
            }

            if (process.exitValue() != 0) {
                log.warn("ffmpeg -version exited with code {}: {}", process.exitValue(), ffmpeg);
                return Optional.empty();
            }

            return output.join().lines()
                    .findFirst()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty());
        } catch (IOException e) {
            log.warn("Cannot run ffmpeg -version: {}", ffmpeg, e);
            return Optional.empty();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private static String readAll(InputStream in) {

        try {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
