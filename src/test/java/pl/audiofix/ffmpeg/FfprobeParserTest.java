package pl.audiofix.ffmpeg;

import org.junit.jupiter.api.Test;
import pl.audiofix.model.MediaInfo;
import pl.audiofix.model.StreamInfo;
import pl.audiofix.model.StreamType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FfprobeParserTest {

    private static final Path FILE = Path.of("film.mkv");

    private final FfprobeParser parser = new FfprobeParser();

    // ---------------------------------------------------------------- stream fields

    @Test
    void parsesAllAudioStreamFields() {
        String json = withStreams("""
                {
                  "index": 1,
                  "codec_name": "dts",
                  "profile": "DTS-HD MA",
                  "codec_type": "audio",
                  "sample_rate": "48000",
                  "channels": 8,
                  "channel_layout": "7.1",
                  "disposition": { "default": 1, "forced": 0 },
                  "tags": { "language": "eng", "title": "DTS-HD MA 7.1" }
                }
                """);

        StreamInfo stream = single(parser.parse(FILE, json));

        assertEquals(1, stream.index());
        assertEquals(StreamType.AUDIO, stream.type());
        assertEquals("dts", stream.codec());
        assertEquals("DTS-HD MA", stream.profile());
        assertEquals(8, stream.channels());
        assertEquals("7.1", stream.channelLayout());
        assertEquals("eng", stream.language());
        assertEquals("DTS-HD MA 7.1", stream.title());
        assertTrue(stream.isDefault());
        assertTrue(stream.isDts());
    }

    @Test
    void mapsCodecTypes() {
        String json = withStreams(
                minimalStream(0, "video", "hevc"),
                minimalStream(1, "audio", "ac3"),
                minimalStream(2, "subtitle", "subrip"),
                minimalStream(3, "attachment", "ttf"),
                minimalStream(4, "data", "bin_data"));

        List<StreamType> types = parser.parse(FILE, json).streams().stream().map(StreamInfo::type).toList();

        assertEquals(List.of(StreamType.VIDEO, StreamType.AUDIO, StreamType.SUBTITLE,
                StreamType.ATTACHMENT, StreamType.OTHER), types);
    }

    @Test
    void streamsKeepOriginalOrderAndIndexes() {
        String json = withStreams(
                minimalStream(0, "video", "hevc"),
                minimalStream(1, "audio", "dts"),
                minimalStream(2, "audio", "ac3"));

        List<Integer> indexes = parser.parse(FILE, json).streams().stream().map(StreamInfo::index).toList();

        assertEquals(List.of(0, 1, 2), indexes);
    }

    @Test
    void missingOptionalFieldsUseDefaults() {
        String json = withStreams(minimalStream(2, "subtitle", "subrip"));

        StreamInfo stream = single(parser.parse(FILE, json));

        assertNull(stream.profile());
        assertEquals(0, stream.channels());
        assertNull(stream.channelLayout());
        assertEquals("und", stream.language());
        assertNull(stream.title());
        assertFalse(stream.isDefault());
    }

    @Test
    void dispositionZeroIsNotDefault() {
        String json = withStreams("""
                { "index": 2, "codec_type": "audio", "codec_name": "ac3",
                  "disposition": { "default": 0 } }
                """);

        assertFalse(single(parser.parse(FILE, json)).isDefault());
    }

    // ---------------------------------------------------------------- tags

    @Test
    void uppercaseTagKeysAreRead() {
        String json = withStreams("""
                { "index": 1, "codec_type": "audio", "codec_name": "ac3",
                  "tags": { "LANGUAGE": "pol", "TITLE": "Lektor", "DURATION": "01:59:00.000000000" } }
                """);

        StreamInfo stream = single(parser.parse(FILE, json));

        assertEquals("pol", stream.language());
        assertEquals("Lektor", stream.title());
    }

    @Test
    void polishCharactersInTitleArePreserved() {
        String json = withStreams("""
                { "index": 1, "codec_type": "audio", "codec_name": "ac3",
                  "tags": { "language": "pol", "title": "Lektor – ąćęłńóśźż" } }
                """);

        assertEquals("Lektor – ąćęłńóśźż", single(parser.parse(FILE, json)).title());
    }

    // ---------------------------------------------------------------- format / whole document

    @Test
    void parsesDurationFromString() {
        String json = withStreams(minimalStream(0, "video", "hevc"));

        assertEquals(7234.56, parser.parse(FILE, json).durationSec(), 0.0001);
    }

    @Test
    void missingFormatGivesZeroDuration() {
        String json = """
                { "streams": [ { "index": 0, "codec_type": "video", "codec_name": "hevc" } ] }
                """;

        assertEquals(0.0, parser.parse(FILE, json).durationSec());
    }

    @Test
    void formatWithoutDurationGivesZeroDuration() {
        String json = """
                { "streams": [], "format": { "filename": "film.mkv" } }
                """;

        assertEquals(0.0, parser.parse(FILE, json).durationSec());
    }

    @Test
    void nonNumericDurationGivesZeroDuration() {
        String json = """
                { "streams": [], "format": { "duration": "N/A" } }
                """;

        assertEquals(0.0, parser.parse(FILE, json).durationSec());
    }

    @Test
    void missingStreamsGivesEmptyList() {
        String json = """
                { "format": { "duration": "10.0" } }
                """;

        assertEquals(List.of(), parser.parse(FILE, json).streams());
    }

    @Test
    void pathIsTakenFromArgumentNotFromJson() {
        String json = withStreams(minimalStream(0, "video", "hevc"));

        assertEquals(FILE, parser.parse(FILE, json).path());
    }

    @Test
    void invalidJsonThrowsFfprobeException() {
        assertThrows(FfprobeException.class, () -> parser.parse(FILE, "this is not json"));
    }

    // ---------------------------------------------------------------- real ffprobe output

    @Test
    void parsesRealSampleWithDts() throws IOException {
        MediaInfo info = parser.parse(FILE, resource("/probe-dts.json"));

        assertFalse(info.streams().isEmpty());
        assertTrue(info.streams().stream().anyMatch(s -> s.type() == StreamType.VIDEO), "no video stream");
        assertTrue(info.audioStreams().stream().anyMatch(StreamInfo::isDts), "no DTS audio stream");
        assertTrue(info.durationSec() > 0, "duration not parsed");
        for (int i = 0; i < info.streams().size(); i++) {
            assertEquals(i, info.streams().get(i).index(), "stream order differs from ffprobe indexes");
        }
        info.streams().forEach(s -> assertNotNull(s.codec(), "missing codec for stream " + s.index()));
    }

    // ---------------------------------------------------------------- helpers

    private static String withStreams(String... streams) {
        return """
                {
                  "streams": [ %s ],
                  "format": { "filename": "ignored.mkv", "duration": "7234.560000", "bit_rate": "25000000" }
                }
                """.formatted(String.join(",", streams));
    }

    private static String minimalStream(int index, String codecType, String codecName) {
        return """
                { "index": %d, "codec_type": "%s", "codec_name": "%s" }
                """.formatted(index, codecType, codecName);
    }

    private static StreamInfo single(MediaInfo info) {
        assertEquals(1, info.streams().size());
        return info.streams().getFirst();
    }

    private static String resource(String name) throws IOException {
        try (InputStream in = FfprobeParserTest.class.getResourceAsStream(name)) {
            assertNotNull(in, "missing test resource " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
