package pl.audiofix.model;

import java.nio.file.Path;
import java.util.List;

public record MediaInfo(Path path, double durationSec, List<StreamInfo> streams, long sizeBytes) {

    public MediaInfo {

        streams = List.copyOf(streams);
    }

    public MediaInfo(Path path, double durationSec, List<StreamInfo> streams) {
        this(path, durationSec, streams, 0);
    }

    public List<StreamInfo> audioStreams() {

        return streamsOf(StreamType.AUDIO);
    }

    public List<StreamInfo> subtitleStreams() {

        return streamsOf(StreamType.SUBTITLE);
    }

    public List<StreamInfo> streamsOf(StreamType type) {

        return streams.stream()
                .filter(stream -> stream.type() == type)
                .toList();
    }
}
