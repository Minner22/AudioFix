package pl.audiofix.ffmpeg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FfmpegVersionTest {

    @TempDir
    Path dir;

    @Test
    void readsFirstLineOfRealFfmpeg() {
        FfmpegPaths paths = TestMedia.requireFfmpeg();

        Optional<String> version = FfmpegVersion.firstLine(paths.ffmpeg());

        assertTrue(version.isPresent());
        assertTrue(version.get().startsWith("ffmpeg version "), version.get());
    }

    @Test
    void missingExecutableGivesEmpty() {
        assertEquals(Optional.empty(), FfmpegVersion.firstLine(dir.resolve("no-such-ffmpeg.exe")));
    }

    @Test
    void fileThatIsNotAProgramGivesEmpty() throws IOException {
        Path fake = Files.writeString(dir.resolve("ffmpeg.exe"), "not a program");

        assertEquals(Optional.empty(), FfmpegVersion.firstLine(fake));
    }

    // ---------------------------------------------------------------- fake programs (.bat)

    @Test
    void firstLineOfAnyProgramIsReturned() throws IOException {
        // checks that the .bat technique below works at all
        Path program = batch("ok.bat", "@echo ffmpeg version test", "@echo second line");

        assertEquals(Optional.of("ffmpeg version test"), FfmpegVersion.firstLine(program));
    }

    @Test
    void programWithoutOutputGivesEmpty() throws IOException {
        Path silent = batch("silent.bat", "@exit /b 0");

        assertEquals(Optional.empty(), FfmpegVersion.firstLine(silent));
    }

    @Test
    void nonZeroExitCodeGivesEmpty() throws IOException {
        Path failing = batch("failing.bat", "@echo ffmpeg version test", "@exit /b 1");

        assertEquals(Optional.empty(), FfmpegVersion.firstLine(failing));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void hangingProgramIsStoppedAfterTimeout() throws IOException {
        // pings localhost for ~5 s while keeping its output open - longer than the 1 s timeout below
        Path hanging = batch("hanging.bat", "@echo ffmpeg version late", "@ping -n 6 127.0.0.1 >nul");

        long start = System.nanoTime();
        Optional<String> version = FfmpegVersion.firstLine(hanging, Duration.ofSeconds(1));
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertEquals(Optional.empty(), version);
        assertTrue(elapsedMs < 4000, "should give up after the timeout, took " + elapsedMs + " ms");
    }

    private Path batch(String name, String... lines) throws IOException {
        return Files.writeString(dir.resolve(name), String.join("\r\n", lines) + "\r\n");
    }
}
