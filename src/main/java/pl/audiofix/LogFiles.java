package pl.audiofix;

import java.nio.file.Path;
import java.util.Map;

public final class LogFiles {

    public static final String PROPERTY = "audiofix.logDir";

    private LogFiles() {}

    public static Path directory() {

        String configured = System.getProperty(PROPERTY);

        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }

        return defaultDirectory(System.getenv(), System.getProperty("user.home"));
    }

    static Path defaultDirectory(Map<String, String> env, String userHome) {

        String localAppData = env.get("LOCALAPPDATA");
        if (localAppData == null || localAppData.isBlank()) {
            return Path.of(userHome, ".audiofix", "logs");
        }

        return Path.of(localAppData, "AudioFix", "logs");
    }

    static void configure() {

        System.setProperty(PROPERTY, directory().toString());
    }
}
