package pl.audiofix.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class TrackTitles {

    private TrackTitles() {}

    private static final List<String> FORMAT_NAMES = List.of(
            "Dolby TrueHD", "TrueHD", "Dolby Atmos", "Atmos",
            "DTS-HD MA", "DTS-HD HRA", "DTS-HD", "DTS:X", "DTS-X", "DTS-ES", "DTS",
            "Dolby Digital Plus", "DD+", "DDP", "E-AC-3", "E-AC3", "EAC3", "Dolby Digital", "DD", "AC-3", "AC3",
            "AAC", "FLAC", "LPCM", "PCM");

    // longest names first, so "DTS-HD MA" wins over "DTS"; no letter/digit around, so "DDR" or "Studio" don't match
    private static final Pattern FORMAT = Pattern.compile(
            "(?<![A-Za-z0-9])(" + FORMAT_NAMES.stream()
                    .sorted(Comparator.comparingInt(String::length).reversed())
                    .map(Pattern::quote)
                    .collect(Collectors.joining("|")) + ")(?![A-Za-z0-9+])",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern LAYOUT = Pattern.compile("(?<![0-9.])\\d\\.\\d(?![0-9.])");
    private static final List<String> SEPARATORS = List.of("/", "|", "-", "–", ",");

    public static Optional<String> forConversion(StreamInfo stream, AudioCodec codec) {

        Optional<String> name = codec.getTitleName();

        if (name.isEmpty() || !stream.isAudio()) {

            return Optional.empty();
        }

        int outChannels = codec.outputChannels(stream.channels());
        boolean downmixed = outChannels < stream.channels();
        String layout = downmixed
                ? layoutName(outChannels, null)
                : layoutName(stream.channels(), stream.channelLayout());

        String title = stream.title();
        if (title == null || title.isBlank()) {
            return Optional.of(layout == null ? name.get() : name.get() + " " + layout);
        }

        Matcher matcher = FORMAT.matcher(title);
        StringBuilder stringBuilder = new StringBuilder();
        boolean found = false;

        while (matcher.find()) {
            matcher.appendReplacement(stringBuilder, found ? "" : Matcher.quoteReplacement(name.get()));
            found = true;
        }
        matcher.appendTail(stringBuilder);

        String result = stringBuilder.toString();
        if (downmixed && layout != null) {
            result = LAYOUT.matcher(result).replaceAll(Matcher.quoteReplacement(layout));
        }

        if (!found && !downmixed) {

            return Optional.empty();
        }

        result = tidy(result);

        return result.equals(title)
                ? Optional.empty()
                : Optional.of(result);
    }

    static String layoutName(int channels, String channelLayout) {

        if (channelLayout != null && !channelLayout.isBlank()) {
            String layout = channelLayout.replaceAll("\\(.*\\)", "").trim();

            return switch (layout) {
                case "mono" -> "1.0";
                case "stereo" -> "2.0";
                default -> layout;
            };
        }

        return switch (channels) {
            case 1 -> "1.0";
            case 2 -> "2.0";
            case 6 -> "5.1";
            case 8 -> "7.1";
            default -> null;
        };
    }

    /** Removes what deleting format names leaves behind: "()", double spaces, separators at the ends or in a row. */
    private static String tidy(String text) {

        String s = text.replaceAll("\\(\\s*\\)", " ").replaceAll("\\s+", " ").trim();
        List<String> tokens = new ArrayList<>();
        for (String token : s.split(" ")) {
            boolean separator = SEPARATORS.contains(token);
            if (separator && (tokens.isEmpty() || SEPARATORS.contains(tokens.getLast()))) {
                continue;
            }
            tokens.add(token);
        }

        while (!tokens.isEmpty() && SEPARATORS.contains(tokens.getLast())) {
            tokens.removeLast();
        }

        return String.join(" ", tokens);
    }

}
