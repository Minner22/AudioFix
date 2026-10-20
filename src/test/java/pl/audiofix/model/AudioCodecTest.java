package pl.audiofix.model;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void titleNameIsShortFormatName() {
        assertEquals(Optional.empty(), AudioCodec.COPY.getTitleName());
        assertEquals(Optional.of("PCM"), AudioCodec.PCM_S24LE.getTitleName());
        assertEquals(Optional.of("PCM"), AudioCodec.PCM_S16LE.getTitleName());
        assertEquals(Optional.of("E-AC3"), AudioCodec.EAC3.getTitleName());
        assertEquals(Optional.of("AC3"), AudioCodec.AC3.getTitleName());
        assertEquals(Optional.of("AAC"), AudioCodec.AAC.getTitleName());
    }

    @Test
    void dolbyCodecsAreLimitedToSixChannels() {
        assertEquals(6, AudioCodec.AC3.outputChannels(8));
        assertEquals(6, AudioCodec.EAC3.outputChannels(8));
        assertEquals(6, AudioCodec.AC3.outputChannels(6));
        assertEquals(2, AudioCodec.EAC3.outputChannels(2));
    }

    @Test
    void otherCodecsKeepAllChannels() {
        assertEquals(8, AudioCodec.PCM_S24LE.outputChannels(8));
        assertEquals(8, AudioCodec.PCM_S16LE.outputChannels(8));
        assertEquals(8, AudioCodec.AAC.outputChannels(8));
        assertEquals(8, AudioCodec.COPY.outputChannels(8));
    }

    @Test
    void everyCodecHasItsNameInBothLanguages() throws java.io.IOException {
        // the model has no texts (#30); the "Akcja" list shows codec.<NAME> from the translation files
        for (String file : java.util.List.of("messages.properties", "messages_en.properties")) {
            java.util.Properties texts = new java.util.Properties();
            try (var in = AudioCodec.class.getResourceAsStream("/pl/audiofix/i18n/" + file)) {
                texts.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            }
            for (AudioCodec codec : AudioCodec.values()) {
                assertTrue(texts.containsKey("codec." + codec.name()), "codec." + codec.name() + " missing in " + file);
            }
        }
    }
}
