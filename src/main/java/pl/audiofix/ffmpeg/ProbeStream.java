package pl.audiofix.ffmpeg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
record ProbeStream(
        int index,
        @JsonProperty("codec_type") String codecType,
        @JsonProperty("codec_name") String codecName,
        String profile,
        Integer channels,
        @JsonProperty("channel_layout") String channelLayout,
        Map<String, String> tags,
        ProbeDisposition disposition
) {
}
