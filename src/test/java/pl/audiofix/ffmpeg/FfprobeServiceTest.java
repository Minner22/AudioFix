package pl.audiofix.ffmpeg;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.audiofix.model.MediaInfo;
import pl.audiofix.model.StreamInfo;
import pl.audiofix.model.StreamType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests with the real ffprobe. Skipped when ffmpeg cannot be found
 * (neither saved in Preferences nor on PATH).
 */
class FfprobeServiceTest {

    @TempDir
    static Path dir;

    private static FfprobeService service;
    private static Path sample;

    @BeforeAll
    static void setUp() throws Exception {
        Optional<FfmpegPaths> paths = new FfmpegLocator().locate();
        assumeTrue(paths.isPresent(), "ffmpeg not found, skipping ffprobe integration tests");

        service = new FfprobeService(paths.get());
        sample = generateSample(paths.get().ffmpeg());
    }

    @Test
    void probesGeneratedFile() {
        MediaInfo info = service.probe(sample);

        assertEquals(sample, info.path());
        assertEquals(1.0, info.durationSec(), 0.1);
        assertEquals(List.of(StreamType.VIDEO, StreamType.AUDIO, StreamType.AUDIO, StreamType.SUBTITLE),
                info.streams().stream().map(StreamInfo::type).toList());

        StreamInfo dts = info.streams().get(1);
        assertTrue(dts.isDts());
        assertEquals("eng", dts.language());
        assertEquals("DTS ąę", dts.title());
        assertTrue(dts.isDefault());

        StreamInfo ac3 = info.streams().get(2);
        assertEquals("ac3", ac3.codec());
        assertEquals("pol", ac3.language());
        assertFalse(ac3.isDefault());

        assertEquals("pol", info.streams().get(3).language());
    }

    @Test
    void nonExistingFileThrowsWithFfprobeMessage() {
        Path missing = dir.resolve("missing.mkv");

        var ex = assertThrows(FfprobeException.class, () -> service.probe(missing));
        assertFalse(ex.getMessage().isBlank());
    }

    @Test
    void textFileDisguisedAsVideoThrowsWithFfprobeMessage() throws IOException {
        Path fake = Files.writeString(dir.resolve("fake.mkv"), "this is not a video");

        var ex = assertThrows(FfprobeException.class, () -> service.probe(fake));
        assertTrue(ex.getMessage().contains("Invalid data"),
                "message should contain ffprobe stderr, was: " + ex.getMessage());
    }

    /**
     * 1-second MKV: video, DTS (eng, default, Polish title), AC3 (pol), SRT subtitles (pol).
     * File name has spaces and Polish characters on purpose.
     */
    private static Path generateSample(Path ffmpeg) throws IOException, InterruptedException {
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
