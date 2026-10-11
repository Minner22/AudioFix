package pl.audiofix.ui;

import pl.audiofix.model.StreamInfo;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Texts and style classes of the track table cells, built from what ffprobe reported. */
final class TrackTexts {

    /** Language shown as "ours" in the language badge; #28-#31 may replace it with the system language. */
    static final String LOCAL_LANGUAGE = "pol";

    private static final Map<String, String> AUDIO_FORMATS = Map.of(
            "ac3", "Dolby Digital",
            "eac3", "Dolby Digital Plus",
            "truehd", "Dolby TrueHD",
            "dts", "DTS",
            "aac", "AAC",
            "flac", "FLAC",
            "opus", "Opus",
            "mp3", "MP3",
            "vorbis", "Vorbis");

    private TrackTexts() {
    }

    /** Upper line of the codec cell: the ffprobe codec name, for video also its profile. */
    static String codecName(StreamInfo stream) {

        if (stream.codec() == null) {
            return "";
        }

        return stream.isAudio() ? stream.codec() : stream.codecLabel();
    }

    /**
     * Lower line of the codec cell for audio, e.g. "DTS-HD MA 5.1" or "Dolby Digital 5.1";
     * empty for video and subtitles, where the upper line says it all.
     */
    static String codecDescription(StreamInfo stream) {

        if (!stream.isAudio()) {
            return "";
        }

        return Stream.of(audioFormat(stream), layout(stream.channelLayout()))
                .filter(part -> !part.isEmpty())
                .collect(Collectors.joining(" "));
    }

    /** "8 ch" for audio, null (shown as "-") for tracks without channels. */
    static String channels(StreamInfo stream) {

        return stream.isAudio() && stream.channels() > 0 ? stream.channels() + " ch" : null;
    }

    /** Style class of the language badge: our language, an unknown one, or any other. */
    static String languageStyleClass(String language) {

        if (LOCAL_LANGUAGE.equals(language)) {
            return "language-local";
        }

        return "und".equals(language) ? "language-unknown" : "language-other";
    }

    /** "5.1(side)" is "5.1", "stereo" is "2.0"; ffprobe's own name for anything else. */
    static String layout(String channelLayout) {

        if (channelLayout == null || channelLayout.isBlank()) {
            return "";
        }

        String layout = channelLayout.strip().toLowerCase(Locale.ROOT);
        return switch (layout) {
            case "mono" -> "1.0";
            case "stereo" -> "2.0";
            default -> {
                int bracket = layout.indexOf('(');
                yield bracket > 0 ? layout.substring(0, bracket) : layout;
            }
        };
    }

    /** The profile when it names the format ("DTS-HD MA"), otherwise the format with the profile ("AAC LC"). */
    private static String audioFormat(StreamInfo stream) {

        String codec = stream.codec() == null ? "" : stream.codec();
        String format = AUDIO_FORMATS.getOrDefault(codec, codec.startsWith("pcm_") ? "PCM" : codec);
        String profile = stream.profile() == null ? "" : stream.profile().strip();

        if (profile.isEmpty()) {
            return format;
        }

        String named = profile.toLowerCase(Locale.ROOT).contains(format.toLowerCase(Locale.ROOT))
                ? profile
                : format + " " + profile;
        return named.replace(" + Dolby Atmos", " + Atmos");
    }
}
