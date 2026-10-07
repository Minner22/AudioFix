package pl.audiofix.ffmpeg;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.audiofix.model.AudioCodec;
import pl.audiofix.model.MediaInfo;
import pl.audiofix.model.StreamInfo;
import pl.audiofix.model.StreamType;
import pl.audiofix.model.TrackPlan;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs commands from CommandBuilder with the real ffmpeg and checks the result with ffprobe.
 * Skipped when ffmpeg cannot be found.
 */
class CommandBuilderIntegrationTest {

    @TempDir
    static Path dir;

    private static FfmpegPaths paths;
    private static FfprobeService probe;
    private static MediaInfo sample;

    private final CommandBuilder builder = new CommandBuilder();

    @BeforeAll
    static void setUp() throws Exception {
        paths = TestMedia.requireFfmpeg();
        probe = new FfprobeService(paths);
        sample = probe.probe(TestMedia.generateSample(paths.ffmpeg(), dir));
    }

    @Test
    void dtsIsConvertedToPcmAndOtherTracksAreKept() throws Exception {
        List<TrackPlan> plans = plansFor(sample);

        MediaInfo result = run(plans, "converted.mkv");

        assertEquals(List.of("mpeg4", "pcm_s24le", "ac3", "subrip"), codecs(result));
        StreamInfo pcm = result.streams().get(1);
        assertEquals("eng", pcm.language());
        assertEquals("DTS ąę", pcm.title());
        assertTrue(pcm.isDefault());
        assertFalse(result.streams().get(2).isDefault());
    }

    @Test
    void removedTrackIsDroppedAndDefaultMoves() throws Exception {
        List<TrackPlan> plans = plansFor(sample);
        plans.get(1).setKeep(false);         // DTS removed
        plans.get(2).setMakeDefault(true);   // AC3 becomes default
        plans.get(3).setMakeDefault(true);   // subtitles become default

        MediaInfo result = run(plans, "removed.mkv");

        assertEquals(List.of(StreamType.VIDEO, StreamType.AUDIO, StreamType.SUBTITLE),
                result.streams().stream().map(StreamInfo::type).toList());
        assertEquals("ac3", result.streams().get(1).codec());
        assertTrue(result.streams().get(1).isDefault());
        assertTrue(result.streams().get(2).isDefault());
    }

    @Test
    void lossyCodecWithBitrateIsAccepted() throws Exception {
        List<TrackPlan> plans = plansFor(sample);
        plans.get(1).setTargetCodec(AudioCodec.EAC3);

        MediaInfo result = run(plans, "eac3.mkv");

        assertEquals("eac3", result.streams().get(1).codec());
    }

    // ---------------------------------------------------------------- helpers

    private static List<TrackPlan> plansFor(MediaInfo info) {
        return info.streams().stream().map(TrackPlan::defaultsFor).toList();
    }

    private MediaInfo run(List<TrackPlan> plans, String outputName) throws Exception {
        Path output = dir.resolve(outputName);
        List<String> args = builder.build(paths.ffmpeg(), sample.path(), output, plans);

        Process p = new ProcessBuilder(args)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)   // -progress pipe:1 writes to stdout
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();

        assertTrue(p.waitFor(60, TimeUnit.SECONDS), "ffmpeg timed out");
        assertEquals(0, p.exitValue(), "ffmpeg failed for: " + args);
        assertTrue(Files.size(output) > 0);
        return probe.probe(output);
    }

    private static List<String> codecs(MediaInfo info) {
        return info.streams().stream().map(StreamInfo::codec).toList();
    }
}
