package pl.audiofix.model;

import java.util.Optional;

public enum AudioCodec {

    COPY("Kopiuj", "copy", null, null, 0),
    PCM_S24LE("PCM 24-bit", "pcm_s24le", null, "PCM", 0),
    PCM_S16LE("PCM 16-bit", "pcm_s16le", null, "PCM", 0),
    EAC3("E-AC3 (Dolby Digital+)", "eac3", "640k", "E-AC3", 6),
    AC3("AC3 (Dolby Digital)", "ac3", "640k", "AC3", 6),
    AAC("AAC (Advanced Audio Coding)", "aac", "320k", "AAC", 0),
    ;

    private final String label;
    private final String ffmpegName;
    private final String bitrate;
    private final String titleName;
    private final int maxChannels;

    AudioCodec(String label, String ffmpegName, String bitrate, String titleName, int maxChannels) {

        this.label = label;
        this.ffmpegName = ffmpegName;
        this.bitrate = bitrate;
        this.titleName = titleName;
        this.maxChannels = maxChannels;
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

    public Optional<String> getTitleName() {

        return Optional.ofNullable(titleName);
    }

    public int outputChannels(int sourceChannels) {

        return maxChannels > 0
                ? Math.min(sourceChannels, maxChannels)
                : sourceChannels;
    }


    @Override

    public String toString() {

        return label;
    }
}
