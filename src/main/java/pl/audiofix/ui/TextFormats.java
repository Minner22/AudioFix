package pl.audiofix.ui;

import pl.audiofix.model.MediaInfo;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class TextFormats {

    private static final String POLISH = "pl";
    private static final List<String> UNITS = List.of("B", "KB", "MB", "GB", "TB");

    private TextFormats() {
    }

    /** "02:15" below an hour, "1:02:15" from an hour on; rounded to whole seconds. */
    static String duration(Duration duration) {

        long seconds = Math.round(duration.toMillis() / 1000.0);
        long hours = seconds / 3600;
        long minutes = seconds / 60 % 60;
        long rest = seconds % 60;
        return hours > 0
                ? String.format("%d:%02d:%02d", hours, minutes, rest)
                : String.format("%02d:%02d", minutes, rest);
    }

    static String fileSize(long bytes) {

        if (bytes < 1024) {
            return bytes + " B";
        }

        double value = bytes;
        int unit = 0;
        while (value >= 1024 && unit < UNITS.size() - 1) {
            value /= 1024;
            unit++;
        }

        return String.format(Messages.locale(), "%.1f %s", value, UNITS.get(unit));   // "1,5 GB", "1.5 GB" (#32)
    }

    static String pluralForm(Locale language, long n) {

        if (n == 1) {
            return "one";
        }
        if (!POLISH.equals(language.getLanguage())) {
            return "many";
        }

        long lastDigit = n % 10;
        long lastTwo = n % 100;
        return lastDigit >= 2 && lastDigit <= 4 && (lastTwo < 12 || lastTwo > 14) ? "few" : "many";
    }

    static Set<String> pluralForms(Locale language) {

        return POLISH.equals(language.getLanguage()) ? Set.of("one", "few", "many") : Set.of("one", "many");
    }

    static String containerName(String formatLongName) {

        if (formatLongName == null) {
            return "";
        }

        String name = formatLongName;
        int slash = name.indexOf(" / ");
        if (slash >= 0) {
            name = name.substring(0, slash);
        }
        int bracket = name.indexOf(" (");
        if (bracket >= 0) {
            name = name.substring(0, bracket);
        }
        return name.strip();
    }

    static String fileInfo(MediaInfo media) {

        String size = media.sizeBytes() > 0 ? fileSize(media.sizeBytes()) : "";
        String parts = Stream.of(size, containerName(media.formatName()))
                .filter(part -> !part.isEmpty())
                .collect(Collectors.joining(", "));
        return parts.isEmpty() ? "" : "(" + parts + ")";
    }
}