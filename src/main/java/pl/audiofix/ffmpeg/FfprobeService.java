package pl.audiofix.ffmpeg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.audiofix.model.MediaInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class FfprobeService {

    private static final Logger log = LoggerFactory.getLogger(FfprobeService.class);

    private final Path ffprobe;
    private final FfprobeParser parser = new FfprobeParser();

    public FfprobeService(FfmpegPaths paths) {

        this.ffprobe = paths.ffprobe();
    }

    public MediaInfo probe(Path file) {

        log.info("Probing {}", file);
        try {
            Process process = new ProcessBuilder(
                    ffprobe.toString(),
                    "-v",
                    "error",
                    "-print_format",
                    "json",
                    "-show_streams",
                    "-show_format",
                    file.toString()
            ).start();

            CompletableFuture<String> stdout = CompletableFuture.supplyAsync(() -> readAll(process.getInputStream()));
            CompletableFuture<String> stderr = CompletableFuture.supplyAsync(() -> readAll(process.getErrorStream()));

            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new FfprobeException("ffprobe nie odpowiada: " + file);
            }

            if (process.exitValue() != 0) {
                String error = stderr.join().trim();
                log.warn("ffprobe failed for {}: {}", file, error);
                error = error.isEmpty() ? "exit code " + process.exitValue() : error;
                throw new FfprobeException("Nie można odczytać pliku " + file + ": " + error);
            }

            return parser.parse(file, stdout.join());
        } catch (IOException e) {
            throw new FfprobeException("Nie można uruchomić ffprobe: " + ffprobe, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FfprobeException("Przerwano odczyt pliku " + file, e);
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
