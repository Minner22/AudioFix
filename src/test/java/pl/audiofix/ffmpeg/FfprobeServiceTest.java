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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        FfmpegPaths paths = TestMedia.requireFfmpeg();

        service = new FfprobeService(paths);
        sample = TestMedia.generateSample(paths.ffmpeg(), dir);
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
}
