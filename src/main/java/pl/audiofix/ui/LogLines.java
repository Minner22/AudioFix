package pl.audiofix.ui;

import java.util.Locale;
import java.util.regex.Pattern;

/** What kind of line the console shows, for its color; and the version line every file log starts with. */
final class LogLines {

    enum Kind {
        VERSION, COMMAND, ERROR, WARNING, SUCCESS, PLAIN;

        /** "log-command", "log-error"…: the line's style class in the console. */
        String styleClass() {
            return "log-" + name().toLowerCase(Locale.ROOT);
        }
    }

    static final String VERSION_PREFIX = "AudioFix ";

    /** The ffmpeg executable at the start of the line (quoted when its path has spaces): the command JobQueue logs. */
    private static final Pattern COMMAND = Pattern.compile("(?i)^(\"[^\"]*ffmpeg(\\.exe)?\"|\\S*ffmpeg(\\.exe)?)\\s");
    private static final Pattern ERROR = Pattern.compile("(?i)^błąd\\b|\\b(error|invalid|failed)\\b");
    private static final Pattern WARNING = Pattern.compile("(?i)^anulowano\\b|\\b(warning|deprecated)\\b");

    private LogLines() {
    }

    static Kind kind(String line) {

        if (line.startsWith(VERSION_PREFIX)) {
            return Kind.VERSION;
        }
        if (COMMAND.matcher(line).lookingAt()) {
            return Kind.COMMAND;
        }
        if (ERROR.matcher(line).find()) {
            return Kind.ERROR;
        }
        if (WARNING.matcher(line).find()) {
            return Kind.WARNING;
        }

        return line.startsWith("Gotowe") ? Kind.SUCCESS : Kind.PLAIN;
    }

    /**
     * "AudioFix 0.4.0 | ffmpeg 7.0.1-essentials_build-www.gyan.dev" from the first line of "ffmpeg -version";
     * just the app when the ffmpeg version is not known yet.
     */
    static String versionLine(String appVersion, String ffmpegVersionLine) {

        String app = VERSION_PREFIX + appVersion;
        if (ffmpegVersionLine == null || ffmpegVersionLine.isBlank()) {
            return app;
        }

        String[] words = ffmpegVersionLine.strip().split("\\s+");
        String version = words.length >= 3 && words[1].equals("version") ? words[2] : ffmpegVersionLine.strip();
        return app + " | ffmpeg " + version;
    }
}
