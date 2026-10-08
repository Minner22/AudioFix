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
        assertEquals("PCM ąę", pcm.title(), "format name in the title follows the new codec");
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

    @Test
    void quickCommandKeepsOneConvertedAudioTrack() throws Exception {
        // the sample has DTS (2 ch, default) and AC3 (1 ch) - ffmpeg picks the one with more channels
        Path output = dir.resolve("quick.mkv");
        List<String> args = builder.buildQuick(paths.ffmpeg(), sample.path(), output);

        MediaInfo result = runCommand(args, output);

        assertEquals(List.of(StreamType.VIDEO, StreamType.AUDIO, StreamType.SUBTITLE),
                result.streams().stream().map(StreamInfo::type).toList());
        assertEquals("pcm_s24le", result.streams().get(1).codec());
        assertEquals(2, result.streams().get(1).channels());
        assertEquals("ass", result.streams().get(2).codec(), "text subtitles are converted to ASS by default");
    }

    @Test
    void sevenOneToAc3BecomesFiveOneWithLfe() throws Exception {
        // AC3 has at most 6 channels; without -ac ffmpeg would pick 5.0(side) and drop the LFE
        MediaInfo surround = probe.probe(generateSevenOne(dir.resolve("surround.mkv")));
        List<TrackPlan> plans = plansFor(surround);
        plans.get(1).setTargetCodec(AudioCodec.AC3);
        Path output = dir.resolve("surround_ac3.mkv");

        MediaInfo result = runCommand(builder.build(paths.ffmpeg(), surround.path(), output, plans), output);

        StreamInfo ac3 = result.streams().get(1);
        assertEquals("ac3", ac3.codec());
        assertEquals(6, ac3.channels());
        assertEquals("5.1(side)", ac3.channelLayout());
        assertEquals("AC3 5.1", ac3.title());
    }

    // ---------------------------------------------------------------- helpers

    /** 1 s of video and a 7.1 FLAC track titled "TrueHD Atmos 7.1". */
    private static Path generateSevenOne(Path output) throws Exception {
        List<String> args = List.of(paths.ffmpeg().toString(), "-hide_banner", "-y",
                "-f", "lavfi", "-i", "testsrc=duration=1:size=320x240:rate=25",
                "-f", "lavfi", "-i", "sine=frequency=440:duration=1:sample_rate=48000",
                "-map", "0", "-map", "1",
                "-af", "aformat=channel_layouts=7.1",
                "-c:v", "mpeg4", "-c:a", "flac",
                "-metadata:s:a:0", "title=TrueHD Atmos 7.1",
                output.toString());
        Process p = new ProcessBuilder(args)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
        assertTrue(p.waitFor(60, TimeUnit.SECONDS), "ffmpeg timed out");
        assertEquals(0, p.exitValue(), "could not generate 7.1 sample");
        return output;
    }

    private static List<TrackPlan> plansFor(MediaInfo info) {
        return info.streams().stream().map(TrackPlan::defaultsFor).toList();
    }

    private MediaInfo run(List<TrackPlan> plans, String outputName) throws Exception {
        Path output = dir.resolve(outputName);
        return runCommand(builder.build(paths.ffmpeg(), sample.path(), output, plans), output);
    }

    private static MediaInfo runCommand(List<String> args, Path output) throws Exception {
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
