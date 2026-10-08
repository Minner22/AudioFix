package pl.audiofix.ui;

import java.nio.file.Path;

record AboutInfo(
        String appVersion,
        Path ffmpegPath,
        String ffmpegVersion,
        String javaVersion,
        String javafxVersion,
        String os
) {

    static final String UNKNOWN = "nieznana";
    private static final String LINE_SEPARATOR = System.lineSeparator();

    static AboutInfo of(String appVersion, Path ffmpegPath, String ffmpegVersion) {

        return new AboutInfo(appVersion,
                ffmpegPath,
                ffmpegVersion,
                Runtime.version().toString(),
                System.getProperty("javafx.runtime.version", UNKNOWN),
                System.getProperty("os.name") + " " + System.getProperty("os.version"));
    }

    AboutInfo withFfmpegVersion(String version) {

        return new AboutInfo(appVersion, ffmpegPath, version, javaVersion, javafxVersion, os);
    }

    String toClipboardText() {

        return "AudioFix " + appVersion + LINE_SEPARATOR +
                "ffmpeg: " + (ffmpegVersion == null ? UNKNOWN : ffmpegVersion) + LINE_SEPARATOR +
                "ffmpeg path: " + (ffmpegPath == null ? UNKNOWN : ffmpegPath) + LINE_SEPARATOR +
                "Java: " + javaVersion + LINE_SEPARATOR +
                "JavaFX: " + javafxVersion + LINE_SEPARATOR +
                "OS: " + os + LINE_SEPARATOR;
    }
}
