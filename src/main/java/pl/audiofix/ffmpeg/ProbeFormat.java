package pl.audiofix.ffmpeg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record ProbeFormat(String duration, String size) {
}
