package pl.audiofix.model;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AudioCodecTest {

    @Test
    void copyAndPcmHaveNoBitrate() {
        assertEquals(Optional.empty(), AudioCodec.COPY.getBitrate());
        assertEquals(Optional.empty(), AudioCodec.PCM_S24LE.getBitrate());
    }

    @Test
    void lossyCodecsHaveBitrate() {
        assertEquals(Optional.of("640k"), AudioCodec.AC3.getBitrate());
        assertEquals(Optional.of("640k"), AudioCodec.EAC3.getBitrate());
    }

    @Test
    void ffmpegNamesMatchEncoders() {
        assertEquals("copy", AudioCodec.COPY.getFfmpegName());
        assertEquals("pcm_s24le", AudioCodec.PCM_S24LE.getFfmpegName());
        assertEquals("eac3", AudioCodec.EAC3.getFfmpegName());
        assertEquals("ac3", AudioCodec.AC3.getFfmpegName());
        assertEquals("aac", AudioCodec.AAC.getFfmpegName());
    }

    @Test
    void toStringIsLabelForComboBox() {
        assertEquals("PCM 24-bit", AudioCodec.PCM_S24LE.toString());
        assertEquals("E-AC3 (Dolby Digital+)", AudioCodec.EAC3.toString());
    }
}
