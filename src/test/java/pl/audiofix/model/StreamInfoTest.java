package pl.audiofix.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamInfoTest {

    @Test
    void missingLanguageBecomesUnd() {
        var stream = audio("ac3", null, null);

        assertEquals("und", stream.language());
    }

    @Test
    void givenLanguageIsKept() {
        var stream = audio("ac3", null, "pol");

        assertEquals("pol", stream.language());
    }

    @Test
    void dtsAudioIsDts() {
        assertTrue(audio("dts", "DTS", "eng").isDts());
    }

    @Test
    void dtsHdMaIsDts() {
        assertTrue(audio("dts", "DTS-HD MA", "eng").isDts());
    }

    @Test
    void ac3IsNotDts() {
        assertFalse(audio("ac3", null, "eng").isDts());
    }

    @Test
    void nonAudioStreamIsNeitherAudioNorDts() {
        var subtitle = new StreamInfo(3, StreamType.SUBTITLE, "subrip", null, 0, null, "pol", null, false);

        assertFalse(subtitle.isAudio());
        assertFalse(subtitle.isDts());
    }

    @Test
    void audioStreamIsAudio() {
        assertTrue(audio("aac", "LC", "eng").isAudio());
    }

    @Test
    void codecLabelIncludesProfile() {
        assertEquals("dts (DTS-HD MA)", audio("dts", "DTS-HD MA", "eng").codecLabel());
    }

    @Test
    void codecLabelWithoutProfileIsCodecOnly() {
        assertEquals("ac3", audio("ac3", null, "eng").codecLabel());
        assertEquals("ac3", audio("ac3", "", "eng").codecLabel());
    }

    // ---------------------------------------------------------------- needsConversion (#18)

    @Test
    void dtsNeedsConversion() {
        assertTrue(audio("dts", "DTS-HD MA", "eng").needsConversion());
    }

    @Test
    void trueHdNeedsConversion() {
        assertTrue(audio("truehd", "Dolby TrueHD + Dolby Atmos", "eng").needsConversion());
    }

    @Test
    void trueHdIsNotDts() {
        assertFalse(audio("truehd", null, "eng").isDts());
    }

    @Test
    void tvFriendlyCodecsDoNotNeedConversion() {
        for (String codec : List.of("ac3", "eac3", "aac", "flac", "pcm_s24le")) {
            assertFalse(audio(codec, null, "eng").needsConversion(), codec);
        }
    }

    @Test
    void nonAudioStreamDoesNotNeedConversion() {
        var other = new StreamInfo(5, StreamType.OTHER, "dts", null, 0, null, "eng", null, false);

        assertFalse(other.needsConversion());
    }

    @Test
    void missingCodecDoesNotNeedConversion() {
        assertFalse(audio(null, null, "eng").needsConversion());
    }

    private static StreamInfo audio(String codec, String profile, String language) {
        return new StreamInfo(1, StreamType.AUDIO, codec, profile, 6, "5.1(side)", language, null, false);
    }
}
