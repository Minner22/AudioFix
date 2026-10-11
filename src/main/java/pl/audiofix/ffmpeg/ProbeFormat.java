package pl.audiofix.ffmpeg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record ProbeFormat(String duration, String size, @JsonProperty("format_long_name") String formatLongName) {
}
