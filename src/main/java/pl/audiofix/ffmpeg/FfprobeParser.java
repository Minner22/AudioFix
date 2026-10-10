package pl.audiofix.ffmpeg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.audiofix.model.MediaInfo;
import pl.audiofix.model.StreamInfo;
import pl.audiofix.model.StreamType;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class FfprobeParser {

    private static final Logger log = LoggerFactory.getLogger(FfprobeParser.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public MediaInfo parse(Path file, String json) {

        ProbeOutput output;

        try {
            output = MAPPER.readValue(json, ProbeOutput.class);
        } catch (JsonProcessingException e) {
            throw new FfprobeException("Nie udało się odczytać danych z ffprobe", e);
        }

        List<StreamInfo> streams = output.streams() == null
                ? List.of()
                : output.streams().stream()
                .map(FfprobeParser::toStreamInfo)
                .toList();

        return new MediaInfo(file, durationSec(output.format()), streams, sizeBytes(output.format()));
    }

    private static long sizeBytes(ProbeFormat format) {

        if (format == null || format.size() == null) {

            return 0;
        }

        try {
            return Math.max(0, Long.parseLong(format.size()));
        } catch (NumberFormatException _) {
            log.warn("Unexpected size from ffprobe: {}", format.size());
            return 0;
        }
    }

    private static double durationSec(ProbeFormat format) {

        if (format == null || format.duration() == null) {
            return 0;
        }

        try {
            return Double.parseDouble(format.duration());
        } catch (NumberFormatException _) {
            log.warn("Unexpected duration from ffprobe: {}", format.duration());
            return 0;
        }
    }

    private static StreamInfo toStreamInfo(ProbeStream s) {

        return new StreamInfo(
                s.index(),
                toStreamType(s.codecType()),
                s.codecName(),
                s.profile(),
                s.channels() == null ? 0 : s.channels(),
                s.channelLayout(),
                tag(s.tags(), "language"),
                tag(s.tags(), "title"),
                s.disposition() != null && s.disposition().isDefault() == 1
        );
    }

    private static StreamType toStreamType(String codecType) {

        if (codecType == null) {
            return StreamType.OTHER;
        }

        return switch (codecType) {
            case "video" -> StreamType.VIDEO;
            case "audio" -> StreamType.AUDIO;
            case "subtitle" -> StreamType.SUBTITLE;
            case "attachment" -> StreamType.ATTACHMENT;
            default -> StreamType.OTHER;
        };
    }

    private static String tag(Map<String, String> tags, String key) {

        if (tags == null) {
            return null;
        }

        for (Map.Entry<String, String> entry : tags.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }

        return null;
    }
}
