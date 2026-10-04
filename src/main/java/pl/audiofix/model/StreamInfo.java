package pl.audiofix.model;

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
}
