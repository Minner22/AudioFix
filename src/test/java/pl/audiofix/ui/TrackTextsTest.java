package pl.audiofix.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import pl.audiofix.model.StreamInfo;
import pl.audiofix.model.StreamType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Cell texts of the track table, from ffprobe data like in real files. */
class TrackTextsTest {

    // ---------------------------------------------------------------- codec, upper line

    @Test
    void audioCodecNameIsJustTheCodec() {
        assertEquals("dts", TrackTexts.codecName(audio("dts", "DTS-HD MA", 6, "5.1(side)")));
    }

    @Test
    void videoCodecNameHasProfile() {
        StreamInfo hevc = new StreamInfo(0, StreamType.VIDEO, "hevc", "Main 10", 0, null, "und", null, true);

        assertEquals("hevc (Main 10)", TrackTexts.codecName(hevc));
    }

    @Test
    void missingCodecIsEmpty() {
        assertEquals("", TrackTexts.codecName(audio(null, null, 2, "stereo")));
    }

    // ---------------------------------------------------------------- codec, lower line

    @ParameterizedTest(name = "{0} / {1} / {2} -> {3}")
    @CsvSource(nullValues = "null", value = {
            "truehd, Dolby TrueHD + Dolby Atmos, 7.1,       Dolby TrueHD + Atmos 7.1",
            "dts,    DTS-HD MA,                  5.1(side), DTS-HD MA 5.1",
            "dts,    DTS,                        5.1(side), DTS 5.1",
            "ac3,    null,                       5.1(side), Dolby Digital 5.1",
            "eac3,   null,                       5.1,       Dolby Digital Plus 5.1",
            "aac,    LC,                         stereo,    AAC LC 2.0",
            "aac,    HE-AAC,                     stereo,    HE-AAC 2.0",
            "flac,   null,                       mono,      FLAC 1.0",
            "pcm_s24le, null,                    7.1,       PCM 7.1",
            "wmapro, null,                       5.1,       wmapro 5.1",
            "ac3,    null,                       null,      Dolby Digital",
    })
    void audioDescriptionNamesFormatAndLayout(String codec, String profile, String layout, String expected) {
        assertEquals(expected, TrackTexts.codecDescription(audio(codec, profile, 6, layout)));
    }

    @Test
    void videoAndSubtitlesHaveNoDescription() {
        StreamInfo video = new StreamInfo(0, StreamType.VIDEO, "hevc", "Main 10", 0, null, "und", null, true);
        StreamInfo subtitles = new StreamInfo(4, StreamType.SUBTITLE, "subrip", null, 0, null, "pol", null, false);

        assertEquals("", TrackTexts.codecDescription(video));
        assertEquals("", TrackTexts.codecDescription(subtitles));
    }

    // ---------------------------------------------------------------- layout

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "5.1(side), 5.1",
            "7.1(wide), 7.1",
            "7.1,       7.1",
            "stereo,    2.0",
            "Stereo,    2.0",
            "mono,      1.0",
            "quad,      quad",
    })
    void layoutIsShortened(String channelLayout, String expected) {
        assertEquals(expected, TrackTexts.layout(channelLayout));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void unknownLayoutIsEmpty(String channelLayout) {
        assertEquals("", TrackTexts.layout(channelLayout));
    }

    // ---------------------------------------------------------------- channels

    @Test
    void audioChannelsAreCount() {
        assertEquals("8 ch", TrackTexts.channels(audio("truehd", null, 8, "7.1")));
    }

    @Test
    void noChannelsForVideoOrUnknownCount() {
        StreamInfo video = new StreamInfo(0, StreamType.VIDEO, "hevc", null, 0, null, "und", null, true);

        assertNull(TrackTexts.channels(video));
        assertNull(TrackTexts.channels(audio("ac3", null, 0, null)), "ffprobe did not say");
    }

    // ---------------------------------------------------------------- language

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "pol, language-local",
            "und, language-unknown",
            "eng, language-other",
            "jpn, language-other",
    })
    void languageBadgeStyle(String language, String expected) {
        assertEquals(expected, TrackTexts.languageStyleClass(language));
    }

    // ---------------------------------------------------------------- helpers

    private static StreamInfo audio(String codec, String profile, int channels, String layout) {
        return new StreamInfo(1, StreamType.AUDIO, codec, profile, channels, layout, "eng", null, false);
    }
}
