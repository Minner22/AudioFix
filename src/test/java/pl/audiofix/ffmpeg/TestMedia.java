package pl.audiofix.ffmpeg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Generates small media files with the real ffmpeg for integration tests.
 */
public final class TestMedia {

    private TestMedia() {
    }

    /**
     * ffmpeg for integration tests. Locally the tests are skipped when ffmpeg is not installed;
     * on CI (GitHub Actions sets {@code CI=true}) a missing ffmpeg is an error, so a broken install
     * step cannot turn the build green with integration tests silently skipped.
     */
    public static FfmpegPaths requireFfmpeg() {
        Optional<FfmpegPaths> found = new FfmpegLocator().locate();
        if (found.isEmpty() && "true".equalsIgnoreCase(System.getenv("CI"))) {
            fail("ffmpeg not found on CI - check the step that installs ffmpeg and adds it to PATH");
        }
        assumeTrue(found.isPresent(), "ffmpeg not found, skipping ffmpeg integration tests");
        return found.get();
    }

    /**
     * 1-second MKV with streams:
     * 0 video (mpeg4), 1 DTS (eng, default, title "DTS ąę"), 2 AC3 (pol), 3 SRT subtitles (pol).
     * File name has spaces and Polish characters on purpose.
     */
    public static Path generateSample(Path ffmpeg, Path dir) throws IOException, InterruptedException {
        Path subs = Files.writeString(dir.resolve("subs.srt"), "1\n00:00:00,000 --> 00:00:01,000\nTest\n");
        Path out = dir.resolve("Film ąę test.mkv");

        Process p = new ProcessBuilder(
                ffmpeg.toString(), "-v", "error", "-y",
                "-f", "lavfi", "-i", "testsrc=duration=1:size=320x240:rate=25",
                "-f", "lavfi", "-i", "sine=frequency=440:duration=1:sample_rate=48000",
                "-f", "lavfi", "-i", "sine=frequency=880:duration=1:sample_rate=48000",
                "-i", subs.toString(),
                "-map", "0", "-map", "1", "-map", "2", "-map", "3",
                "-c:v", "mpeg4",
                "-c:a:0", "dca", "-strict", "-2", "-ac:a:0", "2",
                "-c:a:1", "ac3",
                "-c:s", "srt",
                "-metadata:s:a:0", "language=eng", "-metadata:s:a:0", "title=DTS ąę",
                "-metadata:s:a:1", "language=pol",
                "-metadata:s:s:0", "language=pol",
                "-disposition:a:0", "default", "-disposition:a:1", "0",
                out.toString())
                .inheritIO()
                .start();

        assertTrue(p.waitFor(60, TimeUnit.SECONDS), "ffmpeg timed out while generating sample");
        assertEquals(0, p.exitValue(), "ffmpeg failed to generate sample");
        return out;
    }
}
