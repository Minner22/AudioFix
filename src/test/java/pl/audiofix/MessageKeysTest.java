package pl.audiofix;

import org.junit.jupiter.api.Test;
import pl.audiofix.model.PlanProblem;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Every message the model and ffmpeg can report (#30) has a text in both languages: a key missing in one file
 * would show up only when that error happens, as an exception instead of a message.
 */
class MessageKeysTest {

    private static final Path SOURCES = Path.of("src", "main", "java", "pl", "audiofix");
    private static final Pattern USER_MESSAGE = Pattern.compile("UserMessage\\.of\\(\"([^\"]+)\"");

    @Test
    void everyReportedMessageIsTranslated() throws IOException {
        Set<String> keys = reportedKeys();
        assertFalse(keys.isEmpty(), "no UserMessage.of(...) found in " + SOURCES);

        for (String file : List.of("messages.properties", "messages_en.properties")) {
            Set<String> missing = new TreeSet<>(keys);
            missing.removeAll(load(file).stringPropertyNames());
            assertEquals(Set.of(), missing, "keys reported by the code but missing in " + file);
        }
    }

    @Test
    void everyPlanProblemIsTranslated() throws IOException {
        for (String file : List.of("messages.properties", "messages_en.properties")) {
            Properties texts = load(file);
            for (PlanProblem problem : PlanProblem.values()) {
                assertNotNull(texts.getProperty(problem.messageKey()), problem.messageKey() + " missing in " + file);
            }
        }
    }

    @Test
    void searchFindsTheKeys() throws IOException {
        // the first test is only as good as its search
        Set<String> keys = reportedKeys();

        assertEquals(true, keys.contains("ffmpeg.failed"), keys.toString());
        assertEquals(true, keys.contains("layout.codec"), keys.toString());
        assertEquals(true, keys.contains("error.unexpected"), keys.toString());
    }

    private static Set<String> reportedKeys() throws IOException {
        Set<String> keys = new TreeSet<>();
        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                Matcher key = USER_MESSAGE.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (key.find()) {
                    keys.add(key.group(1));
                }
            }
        }
        return keys;
    }

    private static Properties load(String file) throws IOException {
        try (InputStream in = MessageKeysTest.class.getResourceAsStream("/pl/audiofix/i18n/" + file)) {
            assertNotNull(in, "no translation file " + file);
            Properties texts = new Properties();
            texts.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return texts;
        }
    }
}
