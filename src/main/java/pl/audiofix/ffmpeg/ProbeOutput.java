package pl.audiofix.ffmpeg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record ProbeOutput(List<ProbeStream> streams, ProbeFormat format) {
}
