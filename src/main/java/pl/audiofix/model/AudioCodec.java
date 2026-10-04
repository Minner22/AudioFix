package pl.audiofix.model;

import java.util.Optional;

public enum AudioCodec {

    COPY("Kopiuj", "copy", null),
    PCM_S24LE("PCM 24-bit", "pcm_s24le", null),
    PCM_S16LE("PCM 16-bit", "pcm_s16le", null),
    EAC3("E-AC3 (Dolby Digital+)", "eac3", "640k"),
    AC3("AC3 (Dolby Digital)", "ac3", "640k"),
    AAC("AAC (Advanced Audio Coding)", "aac", "320k");

    private final String label;
    private final String ffmpegName;
    private final String bitrate;

    AudioCodec(String label, String ffmpegName, String bitrate) {

        this.label = label;
        this.ffmpegName = ffmpegName;
        this.bitrate = bitrate;
    }

    public String getLabel() {

        return label;
    }

    public String getFfmpegName() {

        return ffmpegName;
    }

    public Optional<String> getBitrate() {

        return Optional.ofNullable(bitrate);
    }


    @Override
    public String toString() {

        return label;
    }
}
