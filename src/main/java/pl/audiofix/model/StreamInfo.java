package pl.audiofix.model;

import java.util.Set;

public record StreamInfo(
        int index,
        StreamType type,
        String codec,
        String profile,
        int channels,
        String channelLayout,
        String language,
        String title,
        boolean isDefault
) {

    private static final Set<String> CODECS_TO_CONVERT = Set.of("dts", "truehd");

    public StreamInfo {

        if (language == null) {
            language = "und";
        }
    }

    public boolean isDts() {

        return isAudio() && "dts".equals(codec);
    }

    public boolean isAudio() {

        return type == StreamType.AUDIO;
    }

    public String codecLabel() {

        return profile != null && !profile.isBlank()
                ? codec + " (" + profile + ")"
                : codec;
    }

    public boolean needsConversion() {

        return isAudio() && codec != null && CODECS_TO_CONVERT.contains(codec);
    }
}
