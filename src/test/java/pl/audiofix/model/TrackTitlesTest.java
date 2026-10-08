package pl.audiofix.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrackTitlesTest {

    // ---------------------------------------------------------------- titles naming the old format

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource(delimiter = '|', textBlock = """
            TrueHD Atmos 7.1                | PCM 7.1
            DTS-HD MA 5.1                   | PCM 5.1
            Polski DTS 5.1                  | Polski PCM 5.1
            Dolby TrueHD Atmos 7.1          | PCM 7.1
            truehd atmos 7.1                | PCM 7.1
            DTS:X 7.1                       | PCM 7.1
            DTS-ES 6.1                      | PCM 6.1
            Lektor PL DTS                   | Lektor PL PCM
            TrueHD / Atmos - 7.1            | PCM / 7.1
            Dolby Atmos (TrueHD) 7.1        | PCM 7.1
            DTS ąę                          | PCM ąę
            - DTS 5.1                       | PCM 5.1
            / Dolby Atmos /                 | PCM
            """)
    void formatNamesAreReplacedByPcm(String original, String expected) {
        assertEquals(Optional.of(expected), TrackTitles.forConversion(audio("truehd", 8, "7.1", original), AudioCodec.PCM_S24LE));
    }

    @Test
    void ddPlusTitleConvertedFromEac3() {
        StreamInfo eac3 = audio("eac3", 6, "5.1(side)", "Dolby Digital Plus 5.1 (Lektor)");

        assertEquals(Optional.of("PCM 5.1 (Lektor)"), TrackTitles.forConversion(eac3, AudioCodec.PCM_S24LE));
    }

    @Test
    void targetNameDependsOnCodec() {
        StreamInfo dts = audio("dts", 6, "5.1(side)", "DTS-HD MA 5.1");

        assertEquals(Optional.of("E-AC3 5.1"), TrackTitles.forConversion(dts, AudioCodec.EAC3));
        assertEquals(Optional.of("AAC 5.1"), TrackTitles.forConversion(dts, AudioCodec.AAC));
        assertEquals(Optional.of("PCM 5.1"), TrackTitles.forConversion(dts, AudioCodec.PCM_S16LE));
    }

    // ---------------------------------------------------------------- titles without a format name

    @Test
    void titleWithoutFormatNameIsKept() {
        StreamInfo commentary = audio("dts", 6, "5.1(side)", "Commentary by Director");

        assertEquals(Optional.empty(), TrackTitles.forConversion(commentary, AudioCodec.PCM_S24LE));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            Studio mix
            DDR stereo
            Audiodeskrypcja
            Oryginał
            """)
    void wordsOnlyContainingFormatLettersAreNotReplaced(String title) {
        assertEquals(Optional.empty(), TrackTitles.forConversion(audio("dts", 6, "5.1(side)", title), AudioCodec.PCM_S24LE));
    }

    @Test
    void titleAlreadyInTargetFormatIsKept() {
        // e.g. converting a file that was already fixed by AudioFix
        StreamInfo pcm = audio("pcm_s24le", 8, "7.1", "PCM 7.1");

        assertEquals(Optional.empty(), TrackTitles.forConversion(pcm, AudioCodec.PCM_S24LE));
    }

    // ---------------------------------------------------------------- no title

    @ParameterizedTest(name = "{0} ch / {1} -> \"{2}\"")
    @CsvSource(delimiter = '|', nullValues = "null", textBlock = """
            8 | 7.1       | PCM 7.1
            6 | 5.1(side) | PCM 5.1
            6 | 5.1       | PCM 5.1
            2 | stereo    | PCM 2.0
            1 | mono      | PCM 1.0
            6 | null      | PCM 5.1
            3 | null      | PCM
            """)
    void missingTitleIsBuiltFromFormatAndLayout(int channels, String layout, String expected) {
        assertEquals(Optional.of(expected), TrackTitles.forConversion(audio("dts", channels, layout, null), AudioCodec.PCM_S24LE));
    }

    @Test
    void blankTitleIsTreatedAsMissing() {
        assertEquals(Optional.of("PCM 7.1"), TrackTitles.forConversion(audio("truehd", 8, "7.1", "  "), AudioCodec.PCM_S24LE));
    }

    // ---------------------------------------------------------------- AC3 / E-AC3 downmix to 5.1

    @Test
    void sevenOneToAc3IsTitledFiveOne() {
        StreamInfo trueHd = audio("truehd", 8, "7.1", "TrueHD Atmos 7.1");

        assertEquals(Optional.of("AC3 5.1"), TrackTitles.forConversion(trueHd, AudioCodec.AC3));
        assertEquals(Optional.of("E-AC3 5.1"), TrackTitles.forConversion(trueHd, AudioCodec.EAC3));
    }

    @Test
    void sevenOneWithoutTitleToAc3IsFiveOne() {
        assertEquals(Optional.of("AC3 5.1"), TrackTitles.forConversion(audio("truehd", 8, "7.1", null), AudioCodec.AC3));
    }

    @Test
    void downmixFixesLayoutEvenWithoutFormatName() {
        // "Polski 7.1" names no format, but after AC3 conversion it is 5.1 - the title must not lie about channels
        StreamInfo polish = audio("truehd", 8, "7.1", "Polski 7.1");

        assertEquals(Optional.of("Polski 5.1"), TrackTitles.forConversion(polish, AudioCodec.AC3));
    }

    @Test
    void sevenOneToAacKeepsLayout() {
        assertEquals(Optional.of("AAC 7.1"), TrackTitles.forConversion(audio("truehd", 8, "7.1", "TrueHD 7.1"), AudioCodec.AAC));
    }

    // ---------------------------------------------------------------- no change at all

    @Test
    void copyNeverChangesTitle() {
        assertEquals(Optional.empty(), TrackTitles.forConversion(audio("dts", 6, "5.1(side)", "DTS-HD MA 5.1"), AudioCodec.COPY));
    }

    @Test
    void nonAudioStreamIsIgnored() {
        StreamInfo subtitle = new StreamInfo(3, StreamType.SUBTITLE, "subrip", null, 0, null, "pol", "DTS napisy", false);

        assertEquals(Optional.empty(), TrackTitles.forConversion(subtitle, AudioCodec.PCM_S24LE));
    }

    // ---------------------------------------------------------------- helpers

    private static StreamInfo audio(String codec, int channels, String layout, String title) {
        return new StreamInfo(1, StreamType.AUDIO, codec, null, channels, layout, "eng", title, false);
    }
}
