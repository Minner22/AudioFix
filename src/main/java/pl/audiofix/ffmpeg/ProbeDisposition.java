package pl.audiofix.ffmpeg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record ProbeDisposition(@JsonProperty("default") int isDefault) {
}
