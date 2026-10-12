package pl.audiofix.ffmpeg;

import org.junit.jupiter.api.Test;
import pl.audiofix.model.AudioCodec;
import pl.audiofix.model.ConversionJob;
import pl.audiofix.model.ConversionMode;
import pl.audiofix.model.MediaInfo;
import pl.audiofix.model.StreamInfo;
import pl.audiofix.model.StreamType;
import pl.audiofix.model.TrackPlan;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
                "-metadata:s:a:0", "title=PCM 5.1",
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

    // ---------------------------------------------------------------- downmix and titles (#48)

    @Test
    void sevenOneToAc3IsDownmixedToFiveOne() {
        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, convertedTrueHd(AudioCodec.AC3));

        assertTrue(containsSequence(args, "-c:a:0", "ac3", "-b:a:0", "640k", "-ac:a:0", "6"), args.toString());
    }

    @Test
    void sevenOneToEac3IsDownmixedToFiveOne() {
        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, convertedTrueHd(AudioCodec.EAC3));

        assertTrue(containsSequence(args, "-ac:a:0", "6"), args.toString());
    }

    @Test
    void pcmAndAacKeepAllChannels() {
        assertTrue(builder.build(FFMPEG, INPUT, OUTPUT, convertedTrueHd(AudioCodec.PCM_S24LE)).stream().noneMatch(a -> a.startsWith("-ac")));
        assertTrue(builder.build(FFMPEG, INPUT, OUTPUT, convertedTrueHd(AudioCodec.AAC)).stream().noneMatch(a -> a.startsWith("-ac")));
    }

    @Test
    void fiveOneToAc3IsNotDownmixed() {
        List<TrackPlan> plans = typicalPlans();
        plans.get(1).setTargetCodec(AudioCodec.AC3);   // DTS 5.1

        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, plans);

        assertTrue(args.stream().noneMatch(a -> a.startsWith("-ac")), args.toString());
    }

    @Test
    void convertedTrackGetsTitleOfNewFormat() {
        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, convertedTrueHd(AudioCodec.PCM_S24LE));

        assertTrue(containsSequence(args, "-metadata:s:a:0", "title=PCM 7.1"), args.toString());
    }

    @Test
    void downmixedTrackTitleShowsOutputLayout() {
        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, convertedTrueHd(AudioCodec.AC3));

        assertTrue(containsSequence(args, "-metadata:s:a:0", "title=AC3 5.1"), args.toString());
    }

    @Test
    void titleIndexCountsOnlyKeptAudioTracks() {
        // 0 video, 1 AC3 (removed), 2 TrueHD -> TrueHD becomes output audio #0
        List<TrackPlan> plans = List.of(
                TrackPlan.defaultsFor(stream(0, StreamType.VIDEO, "hevc", true)),
                TrackPlan.defaultsFor(stream(1, StreamType.AUDIO, "ac3", true)),
                TrackPlan.defaultsFor(audio(2, "truehd", 8, "7.1", "TrueHD Atmos 7.1")));
        plans.get(1).setKeep(false);

        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, plans);

        assertTrue(containsSequence(args, "-metadata:s:a:0", "title=PCM 7.1"), args.toString());
        assertFalse(args.contains("-metadata:s:a:1"));
    }

    @Test
    void copiedTrackKeepsItsTitle() {
        List<TrackPlan> plans = convertedTrueHd(AudioCodec.COPY);

        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, plans);

        assertTrue(args.stream().noneMatch(a -> a.startsWith("-metadata")), args.toString());
    }

    @Test
    void titleWithoutFormatNameIsNotChanged() {
        List<TrackPlan> plans = List.of(
                TrackPlan.defaultsFor(stream(0, StreamType.VIDEO, "hevc", true)),
                TrackPlan.defaultsFor(audio(1, "dts", 6, "5.1(side)", "Komentarz reżysera")));

        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, plans);

        assertTrue(args.contains("-c:a:0"), "track is still converted");
        assertTrue(args.stream().noneMatch(a -> a.startsWith("-metadata")), args.toString());
    }

    @Test
    void titleWithSpacesIsOneArgument() {
        List<String> args = builder.build(FFMPEG, INPUT, OUTPUT, convertedTrueHd(AudioCodec.PCM_S24LE));

        assertEquals("title=PCM 7.1", args.get(args.indexOf("-metadata:s:a:0") + 1));
    }

    @Test
    void quickCommandRenamesTheTrackFfmpegPicks() {
        // #69: the converted track kept "TrueHD Atmos 7.1" although it is PCM now
        List<String> args = builder.buildQuick(FFMPEG, media(audio(1, "truehd", 8, "7.1", "TrueHD Atmos 7.1")), OUTPUT);

        assertTrue(containsSequence(args, "-metadata:s:a:0", "title=PCM 7.1"), args.toString());
    }

    @Test
    void quickCommandKeepsTitleWithoutFormatName() {
        // nothing to rename in "Komentarz reżysera"; ffmpeg copies it as it is
        List<String> args = builder.buildQuick(FFMPEG, media(audio(1, "dts", 6, "5.1", "Komentarz reżysera")), OUTPUT);

        assertTrue(args.stream().noneMatch(a -> a.startsWith("-metadata")), args.toString());
    }

    @Test
    void quickCommandWithoutAudioHasNoTitle() {
        List<String> args = builder.buildQuick(FFMPEG, media(), OUTPUT);

        assertTrue(args.stream().noneMatch(a -> a.startsWith("-metadata")), args.toString());
    }

    @Test
    void quickCommandDoesNotDownmix() {
        // PCM takes any number of channels; -ac belongs to AC3 / E-AC3 in a planned conversion
        List<String> args = builder.buildQuick(FFMPEG, media(audio(1, "truehd", 8, "7.1", "TrueHD Atmos 7.1")), OUTPUT);

        assertTrue(args.stream().noneMatch(a -> a.startsWith("-ac")), args.toString());
    }

    // ---------------------------------------------------------------- ffmpeg's own audio choice (#69)

    @Test
    void defaultTrackWinsOverMoreChannels() {
        // checked on ffmpeg 7.0.1: a default stereo track is taken before a 7.1 one
        StreamInfo stereo = new StreamInfo(1, StreamType.AUDIO, "ac3", null, 2, "stereo", "pol", null, true);
        StreamInfo sevenOne = new StreamInfo(2, StreamType.AUDIO, "truehd", null, 8, "7.1", "eng", null, false);

        assertEquals(Optional.of(stereo), CommandBuilder.autoSelectedAudio(List.of(stereo, sevenOne)));
    }

    @Test
    void withoutDefaultMostChannelsWin() {
        StreamInfo stereo = new StreamInfo(1, StreamType.AUDIO, "ac3", null, 2, "stereo", "pol", null, false);
        StreamInfo sevenOne = new StreamInfo(2, StreamType.AUDIO, "truehd", null, 8, "7.1", "eng", null, false);

        assertEquals(Optional.of(sevenOne), CommandBuilder.autoSelectedAudio(List.of(stereo, sevenOne)));
    }

    @Test
    void firstOfEqualTracksWins() {
        StreamInfo first = new StreamInfo(1, StreamType.AUDIO, "dts", null, 6, "5.1", "eng", null, true);
        StreamInfo second = new StreamInfo(2, StreamType.AUDIO, "ac3", null, 6, "5.1", "pol", null, true);

        assertEquals(Optional.of(first), CommandBuilder.autoSelectedAudio(List.of(first, second)));
    }

    @Test
    void onlyAudioCounts() {
        StreamInfo video = new StreamInfo(0, StreamType.VIDEO, "hevc", null, 0, null, "und", null, true);
        StreamInfo subtitles = new StreamInfo(2, StreamType.SUBTITLE, "subrip", null, 0, null, "pol", null, true);

        assertEquals(Optional.empty(), CommandBuilder.autoSelectedAudio(List.of(video, subtitles)));
    }

    // ---------------------------------------------------------------- queued job (#12)

    @Test
    void plannedJobUsesItsTrackPlans() {
        List<TrackPlan> plans = typicalPlans();
        plans.get(2).setKeep(false);
        ConversionJob job = new ConversionJob(new MediaInfo(INPUT, 60, List.of()), plans, OUTPUT, ConversionMode.PLANNED);

        assertEquals(builder.build(FFMPEG, INPUT, OUTPUT, plans), builder.build(FFMPEG, job));
    }

    @Test
    void quickJobUsesQuickCommand() {
        MediaInfo media = media(audio(1, "truehd", 8, "7.1", "TrueHD Atmos 7.1"));
        ConversionJob job = new ConversionJob(media, typicalPlans(), OUTPUT, ConversionMode.QUICK);

        assertEquals(builder.buildQuick(FFMPEG, media, OUTPUT), builder.build(FFMPEG, job));
    }

    @Test
    void jobCommandWritesToCurrentOutput() {
        ConversionJob job = new ConversionJob(new MediaInfo(INPUT, 60, List.of()), typicalPlans(), OUTPUT, ConversionMode.PLANNED);
        Path changed = Path.of("E:\\Wynik.mkv");
        job.setOutput(changed);

        assertEquals(changed.toString(), builder.build(FFMPEG, job).getLast());
    }

    // ---------------------------------------------------------------- buildQuick (#21)

    @Test
    void quickCommandIsTheOldManualCommandWithNewTitle() {
        List<String> args = builder.buildQuick(FFMPEG, media(audio(1, "truehd", 8, "7.1", "TrueHD Atmos 7.1")), OUTPUT);

        assertEquals(List.of(
                FFMPEG.toString(), "-hide_banner", "-y",
                "-i", INPUT.toString(),
                "-c:v", "copy",
                "-c:a", "pcm_s24le",
                "-metadata:s:a:0", "title=PCM 7.1",
                "-progress", "pipe:1", "-nostats",
                OUTPUT.toString()), args);
    }

    @Test
    void quickCommandLetsFfmpegChooseStreams() {
        // no -map: ffmpeg's own stream selection, exactly like the command used before AudioFix
        List<String> args = builder.buildQuick(FFMPEG, media(audio(1, "truehd", 8, "7.1", "TrueHD Atmos 7.1")), OUTPUT);

        assertFalse(args.contains("-map"));
        assertFalse(args.contains("-map_chapters"));
        assertTrue(args.stream().noneMatch(a -> a.startsWith("-disposition")));
    }

    @Test
    void quickCommandKeepsPathsWithSpacesAsSingleArguments() {
        List<String> args = builder.buildQuick(FFMPEG, media(), OUTPUT);

        assertTrue(args.contains(INPUT.toString()));
        assertEquals(OUTPUT.toString(), args.getLast());
    }

    // ---------------------------------------------------------------- toCommandLine (#11)

    @Test
    void commandLineJoinsArgumentsWithSpaces() {
        assertEquals("ffmpeg -i in.mkv -c copy out.mkv",
                CommandBuilder.toCommandLine(List.of("ffmpeg", "-i", "in.mkv", "-c", "copy", "out.mkv")));
    }

    @Test
    void commandLineQuotesArgumentsWithSpaces() {
        assertEquals("ffmpeg -i \"D:\\Filmy\\Mój film.mkv\" \"D:\\Filmy\\Mój film_fixed.mkv\"",
                CommandBuilder.toCommandLine(List.of("ffmpeg", "-i", "D:\\Filmy\\Mój film.mkv", "D:\\Filmy\\Mój film_fixed.mkv")));
    }

    @Test
    void commandLineOfBuiltCommandStartsWithFfmpegAndEndsWithOutput() {
        String line = CommandBuilder.toCommandLine(builder.build(FFMPEG, INPUT, OUTPUT, typicalPlans()));

        assertTrue(line.startsWith("C:\\ffmpeg\\bin\\ffmpeg.exe -hide_banner"), line);
        assertTrue(line.endsWith("\"D:\\Filmy\\Mój film_fixed.mkv\""), line);
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

    /** 0 video, 1 TrueHD 7.1 "TrueHD Atmos 7.1" converted to the given codec. */
    private static List<TrackPlan> convertedTrueHd(AudioCodec codec) {
        List<TrackPlan> plans = List.of(
                TrackPlan.defaultsFor(stream(0, StreamType.VIDEO, "hevc", true)),
                TrackPlan.defaultsFor(audio(1, "truehd", 8, "7.1", "TrueHD Atmos 7.1")));
        plans.get(1).setTargetCodec(codec);
        return plans;
    }

    /** INPUT with a video stream and the given streams after it. */
    private static MediaInfo media(StreamInfo... streams) {
        List<StreamInfo> all = new java.util.ArrayList<>(List.of(stream(0, StreamType.VIDEO, "hevc", true)));
        all.addAll(List.of(streams));
        return new MediaInfo(INPUT, 60, all);
    }

    private static StreamInfo audio(int index, String codec, int channels, String layout, String title) {
        return new StreamInfo(index, StreamType.AUDIO, codec, null, channels, layout, "eng", title, true);
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
