package pl.audiofix.model;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaInfoTest {

    private static final StreamInfo VIDEO = new StreamInfo(0, StreamType.VIDEO, "hevc", "Main 10", 0, null, "und", null, true);
    private static final StreamInfo DTS = new StreamInfo(1, StreamType.AUDIO, "dts", "DTS-HD MA", 8, "7.1", "eng", null, true);
    private static final StreamInfo AC3 = new StreamInfo(2, StreamType.AUDIO, "ac3", null, 6, "5.1(side)", "pol", "Lektor", false);
    private static final StreamInfo SUBS = new StreamInfo(3, StreamType.SUBTITLE, "subrip", null, 0, null, "pol", null, false);

    @Test
    void audioStreamsReturnsOnlyAudioInOriginalOrder() {
        var info = mediaInfo(List.of(VIDEO, DTS, AC3, SUBS));

        assertEquals(List.of(DTS, AC3), info.audioStreams());
    }

    @Test
    void subtitleStreamsReturnsOnlySubtitles() {
        var info = mediaInfo(List.of(VIDEO, DTS, AC3, SUBS));

        assertEquals(List.of(SUBS), info.subtitleStreams());
    }

    @Test
    void streamsOfTypeNotPresentIsEmpty() {
        var info = mediaInfo(List.of(VIDEO, DTS));

        assertEquals(List.of(), info.streamsOf(StreamType.ATTACHMENT));
    }

    @Test
    void streamsAreDefensivelyCopied() {
        var source = new ArrayList<>(List.of(VIDEO, DTS));
        var info = mediaInfo(source);

        source.add(SUBS);

        assertEquals(2, info.streams().size());
    }

    @Test
    void streamsAreUnmodifiable() {
        var info = mediaInfo(List.of(VIDEO, DTS));

        assertThrows(UnsupportedOperationException.class, () -> info.streams().add(SUBS));
    }

    @Test
    void sizeIsKept() {
        var info = new MediaInfo(Path.of("film.mkv"), 7200.0, List.of(VIDEO), 34_789_235_712L);

        assertEquals(34_789_235_712L, info.sizeBytes());
    }

    @Test
    void sizeIsZeroWhenNotGiven() {
        assertEquals(0, mediaInfo(List.of(VIDEO)).sizeBytes());
    }

    private static MediaInfo mediaInfo(List<StreamInfo> streams) {
        return new MediaInfo(Path.of("film.mkv"), 7200.0, streams);
    }
}
