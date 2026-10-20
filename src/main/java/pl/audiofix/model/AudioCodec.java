package pl.audiofix.model;

import java.util.Optional;

public enum AudioCodec {

    COPY("copy", null, null, 0),
    PCM_S24LE("pcm_s24le", null, "PCM", 0),
    PCM_S16LE("pcm_s16le", null, "PCM", 0),
    EAC3("eac3", "640k", "E-AC3", 6),
    AC3("ac3", "640k", "AC3", 6),
    AAC("aac", "320k", "AAC", 0);

    private final String ffmpegName;
    private final String bitrate;
    private final String titleName;
    private final int maxChannels;

    AudioCodec(String ffmpegName, String bitrate, String titleName, int maxChannels) {

        this.ffmpegName = ffmpegName;
        this.bitrate = bitrate;
        this.titleName = titleName;
        this.maxChannels = maxChannels;
    }

    public String getFfmpegName() {

        return ffmpegName;
    }

    public Optional<String> getBitrate() {

        return Optional.ofNullable(bitrate);
    }

    /** Short format name used in the track title, e.g. "PCM"; empty for COPY. */
    public Optional<String> getTitleName() {

        return Optional.ofNullable(titleName);
    }

    /** Channels in the converted track: the source count, limited by what the encoder supports. */
    public int outputChannels(int sourceChannels) {

        return maxChannels > 0
                ? Math.min(sourceChannels, maxChannels)
                : sourceChannels;
    }
}
