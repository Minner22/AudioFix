package pl.audiofix.ffmpeg;

import org.junit.jupiter.api.Test;
import pl.audiofix.model.AudioCodec;
import pl.audiofix.model.StreamInfo;
import pl.audiofix.model.StreamType;
import pl.audiofix.model.TrackPlan;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandBuilderTest {

    private static final Path FFMPEG = Path.of("C:\\ffmpeg\\bin\\ffmpeg.exe");
    private static final Path INPUT = Path.of("D:\\Filmy\\Mój film.mkv");
    private static final Path OUTPUT = Path.of("D:\\Filmy\\Mój film_fixed.mkv");

    private final CommandBuilder builder = new CommandBuilder();

    // ---------------------------------------------------------------- whole command

    @Test
    void fullCommandForTypicalDtsFile() {
        // 0 video, 1 DTS (default), 2 AC3, 3 subtitles
        List<TrackPlan> plans = typicalPlans();

        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, plans);

        assertEquals(List.of(
                "C:\\ffmpeg\\bin\\ffmpeg.exe", "-hide_banner", "-y",
                "-i", "D:\\Filmy\\Mój film.mkv",
                "-map", "0:0", "-map", "0:1", "-map", "0:2", "-map", "0:3",
                "-map", "0:t?",
                "-map_chapters", "0",
                "-c", "copy",
                "-c:a:0", "pcm_s24le",
                "-disposition:a:0", "+default",
                "-disposition:a:1", "-default",
                "-disposition:s:0", "-default",
                "-progress", "pipe:1", "-nostats",
                "D:\\Filmy\\Mój film_fixed.mkv"), args);
    }

    @Test
    void pathsWithSpacesArePassedAsSingleArguments() {
        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, typicalPlans());

        assertTrue(args.contains("D:\\Filmy\\Mój film.mkv"));
        assertEquals("D:\\Filmy\\Mój film_fixed.mkv", args.getLast());
    }

    @Test
    void progressOptionsComeRightBeforeOutput() {
        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, typicalPlans());

        assertEquals(List.of("-progress", "pipe:1", "-nostats", OUTPUT.toString()),
                args.subList(args.size() - 4, args.size()));
    }

    @Test
    void streamCopyIsDeclaredBeforeCodecOverrides() {
        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, typicalPlans());

        assertTrue(indexOfSequence(args, "-c", "copy") < indexOfSequence(args, "-c:a:0", "pcm_s24le"),
                "-c copy must come first, otherwise it overrides -c:a:<k>");
    }

    // ---------------------------------------------------------------- mapping

    @Test
    void removedTracksAreNotMapped() {
        List<TrackPlan> plans = typicalPlans();
        plans.get(2).setKeep(false);   // AC3
        plans.get(3).setKeep(false);   // subtitles

        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, plans);

        assertEquals(List.of("0:0", "0:1", "0:t?"), mappedStreams(args));
    }

    @Test
    void attachmentsAndChaptersAreAlwaysCopied() {
        List<TrackPlan> plans = typicalPlans();
        plans.add(TrackPlan.defaultsFor(stream(4, StreamType.ATTACHMENT, "ttf", false)));

        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, plans);

        assertTrue(containsSequence(args, "-map", "0:t?"));
        assertTrue(containsSequence(args, "-map_chapters", "0"));
        assertFalse(mappedStreams(args).contains("0:4"), "attachments are covered by 0:t?, not mapped one by one");
    }

    @Test
    void dataStreamsAreNotMapped() {
        List<TrackPlan> plans = typicalPlans();
        plans.add(TrackPlan.defaultsFor(stream(4, StreamType.OTHER, "bin_data", false)));

        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, plans);

        assertFalse(mappedStreams(args).contains("0:4"));
    }

    @Test
    void mapsUseAbsoluteStreamIndexes() {
        // stream indexes in the file do not have to start at 0 or be contiguous
        List<TrackPlan> plans = List.of(
                TrackPlan.defaultsFor(stream(0, StreamType.VIDEO, "hevc", true)),
                TrackPlan.defaultsFor(stream(3, StreamType.AUDIO, "dts", true)),
                TrackPlan.defaultsFor(stream(7, StreamType.SUBTITLE, "subrip", false)));

        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, plans);

        assertEquals(List.of("0:0", "0:3", "0:7", "0:t?"), mappedStreams(args));
    }

    // ---------------------------------------------------------------- codecs

    @Test
    void copiedAudioHasNoCodecOverride() {
        List<TrackPlan> plans = typicalPlans();
        plans.get(1).setTargetCodec(AudioCodec.COPY);   // keep DTS as is

        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, plans);

        assertTrue(args.stream().noneMatch(a -> a.startsWith("-c:a:")));
        assertTrue(args.stream().noneMatch(a -> a.startsWith("-b:a:")));
    }

    @Test
    void codecIndexCountsOnlyKeptAudioTracks() {
        // 0 video, 1 AC3 (removed), 2 DTS -> DTS becomes output audio #0
        List<TrackPlan> plans = List.of(
                TrackPlan.defaultsFor(stream(0, StreamType.VIDEO, "hevc", true)),
                TrackPlan.defaultsFor(stream(1, StreamType.AUDIO, "ac3", true)),
                TrackPlan.defaultsFor(stream(2, StreamType.AUDIO, "dts", false)));
        plans.get(1).setKeep(false);

        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, plans);

        assertTrue(containsSequence(args, "-c:a:0", "pcm_s24le"), args.toString());
        assertFalse(args.contains("-c:a:1"));
    }

    @Test
    void codecIndexIsNotTheInputStreamIndex() {
        // 0 video, 1 AC3 (copy), 2 DTS -> DTS is output audio #1, not #2
        List<TrackPlan> plans = List.of(
                TrackPlan.defaultsFor(stream(0, StreamType.VIDEO, "hevc", true)),
                TrackPlan.defaultsFor(stream(1, StreamType.AUDIO, "ac3", true)),
                TrackPlan.defaultsFor(stream(2, StreamType.AUDIO, "dts", false)));

        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, plans);

        assertTrue(containsSequence(args, "-c:a:1", "pcm_s24le"), args.toString());
    }

    @Test
    void multipleTracksCanBeConverted() {
        // TrueHD and DTS in one file (like the Drive remux)
        List<TrackPlan> plans = List.of(
                TrackPlan.defaultsFor(stream(0, StreamType.VIDEO, "hevc", true)),
                TrackPlan.defaultsFor(stream(1, StreamType.AUDIO, "truehd", true)),
                TrackPlan.defaultsFor(stream(2, StreamType.AUDIO, "ac3", false)),
                TrackPlan.defaultsFor(stream(3, StreamType.AUDIO, "dts", false)));

        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, plans);

        assertTrue(containsSequence(args, "-c:a:0", "pcm_s24le"));
        assertFalse(args.contains("-c:a:1"));
        assertTrue(containsSequence(args, "-c:a:2", "pcm_s24le"));
    }

    @Test
    void lossyCodecAddsBitrate() {
        List<TrackPlan> plans = typicalPlans();
        plans.get(1).setTargetCodec(AudioCodec.AC3);

        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, plans);

        assertTrue(containsSequence(args, "-c:a:0", "ac3", "-b:a:0", "640k"), args.toString());
    }

    @Test
    void pcmHasNoBitrate() {
        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, typicalPlans());

        assertTrue(args.stream().noneMatch(a -> a.startsWith("-b:a:")));
    }

    // ---------------------------------------------------------------- dispositions

    @Test
    void everyKeptAudioTrackGetsDisposition() {
        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, typicalPlans());

        assertTrue(containsSequence(args, "-disposition:a:0", "+default"));
        assertTrue(containsSequence(args, "-disposition:a:1", "-default"));
    }

    @Test
    void changedDefaultAudioIsApplied() {
        List<TrackPlan> plans = typicalPlans();
        plans.get(1).setMakeDefault(false);   // DTS
        plans.get(2).setMakeDefault(true);    // AC3

        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, plans);

        assertTrue(containsSequence(args, "-disposition:a:0", "-default"));
        assertTrue(containsSequence(args, "-disposition:a:1", "+default"));
    }

    @Test
    void dispositionIndexCountsOnlyKeptTracks() {
        List<TrackPlan> plans = typicalPlans();
        plans.get(1).setKeep(false);         // DTS removed
        plans.get(2).setMakeDefault(true);   // AC3 becomes output audio #0 and default

        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, plans);

        assertTrue(containsSequence(args, "-disposition:a:0", "+default"));
        assertFalse(args.contains("-disposition:a:1"));
    }

    @Test
    void defaultSubtitleIsApplied() {
        List<TrackPlan> plans = typicalPlans();
        plans.get(3).setMakeDefault(true);

        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, plans);

        assertTrue(containsSequence(args, "-disposition:s:0", "+default"));
    }

    @Test
    void noSubtitlesKeptMeansNoSubtitleDisposition() {
        List<TrackPlan> plans = typicalPlans();
        plans.get(3).setKeep(false);

        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, plans);

        assertTrue(args.stream().noneMatch(a -> a.startsWith("-disposition:s:")));
    }

    @Test
    void videoDispositionIsNotTouched() {
        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, typicalPlans());

        assertTrue(args.stream().noneMatch(a -> a.startsWith("-disposition:v")));
    }

    // ---------------------------------------------------------------- helpers

    /** 0 video, 1 DTS (default), 2 AC3, 3 subtitles - with defaults from TrackPlan.defaultsFor. Mutable. */
    private static List<TrackPlan> typicalPlans() {
        return new java.util.ArrayList<>(List.of(
                TrackPlan.defaultsFor(stream(0, StreamType.VIDEO, "hevc", true)),
                TrackPlan.defaultsFor(stream(1, StreamType.AUDIO, "dts", true)),
                TrackPlan.defaultsFor(stream(2, StreamType.AUDIO, "ac3", false)),
                TrackPlan.defaultsFor(stream(3, StreamType.SUBTITLE, "subrip", false))));
    }

    private static StreamInfo stream(int index, StreamType type, String codec, boolean isDefault) {
        int channels = type == StreamType.AUDIO ? 6 : 0;
        return new StreamInfo(index, type, codec, null, channels, null, "eng", null, isDefault);
    }

    /** Values that follow each "-map" argument, in order. */
    private static List<String> mappedStreams(List<String> args) {
        List<String> maps = new java.util.ArrayList<>();
        for (int i = 0; i < args.size() - 1; i++) {
            if (args.get(i).equals("-map")) {
                maps.add(args.get(i + 1));
            }
        }
        return maps;
    }

    private static boolean containsSequence(List<String> args, String... sequence) {
        return indexOfSequence(args, sequence) >= 0;
    }

    private static int indexOfSequence(List<String> args, String... sequence) {
        return Collections.indexOfSubList(args, List.of(sequence));
    }
}
