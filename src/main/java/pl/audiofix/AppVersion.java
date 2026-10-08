package pl.audiofix;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppVersion {

    static final String UNKNOWN = "dev";
    private static final String CURRENT = load();

    private AppVersion() {

    }

    public static String current() {

        return CURRENT;
    }

    private static String load() {

        try (InputStream inputStream = AppVersion.class.getResourceAsStream("version.properties")) {

            if (inputStream == null) {
                return UNKNOWN;
            }

            Properties properties = new Properties();
            properties.load(inputStream);

            String version = properties.getProperty("version", "").trim();

            if (version.isEmpty() || version.contains("${")) {
                return UNKNOWN;
            }

            return version;
        } catch (IOException _) {
            return UNKNOWN;
        }


    }
}
