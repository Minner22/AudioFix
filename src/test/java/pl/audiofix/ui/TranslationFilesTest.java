package pl.audiofix.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The translation files against each other and against the FXML files that use them (#28). */
class TranslationFilesTest {

    private static final List<String> FILES = List.of("messages.properties", "messages_en.properties");
    private static final Path FXML_FOLDER = Path.of("src", "main", "resources", "pl", "audiofix", "ui");
    private static final Pattern FXML_TEXT = Pattern.compile("\\b(text|promptText)=\"([^\"]*)\"");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\d+[^}]*}");
    /** A counted text for Messages.count: "tracks.count.few" is the "few" form of "tracks.count" (#32). */
    private static final Pattern COUNTED = Pattern.compile("(.+\\.count)\\.([a-z]+)");

    // ---------------------------------------------------------------- the two languages

    @Test
    void bothLanguagesHaveTheSameKeys() throws IOException {
        // counted texts are compared by name only: each language has its own noun forms, see below
        Set<String> polish = withoutForms(load("messages.properties").stringPropertyNames());
        Set<String> english = withoutForms(load("messages_en.properties").stringPropertyNames());

        assertEquals(polish, english, "a key missing in one of the languages");
    }

    @ParameterizedTest
    @CsvSource({"messages.properties, pl", "messages_en.properties, en"})
    void countedTextsHaveExactlyTheFormsOfTheirLanguage(String file, String language) throws IOException {
        // a missing form throws MissingResourceException for some counts only, e.g. "22 ścieżki" but not "5 ścieżek"
        Map<String, Set<String>> forms = new TreeMap<>();
        for (String key : load(file).stringPropertyNames()) {
            Matcher counted = COUNTED.matcher(key);
            if (counted.matches()) {
                forms.computeIfAbsent(counted.group(1), name -> new TreeSet<>()).add(counted.group(2));
            }
        }

        assertTrue(forms.containsKey("tracks.count"), forms.toString());
        forms.forEach((name, found) ->
                assertEquals(new TreeSet<>(TextFormats.pluralForms(Locale.of(language))), found, name + " in " + file));
    }

    @ParameterizedTest
    @ValueSource(strings = {"messages.properties", "messages_en.properties"})
    void noTextIsEmpty(String file) throws IOException {
        Properties texts = load(file);

        for (String key : texts.stringPropertyNames()) {
            assertFalse(texts.getProperty(key).isBlank(), key + " is empty in " + file);
        }
    }

    @Test
    void bothLanguagesHaveTheSamePlaceholders() throws IOException {
        // "Wczytano {0}: {1}" translated as "Loaded {0}" would silently drop the number
        Properties polish = load("messages.properties");
        Properties english = load("messages_en.properties");

        for (String key : polish.stringPropertyNames()) {
            if (english.containsKey(key)) {   // "tracks.count.few" is Polish only
                assertEquals(placeholders(polish.getProperty(key)), placeholders(english.getProperty(key)), key);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"messages.properties", "messages_en.properties"})
    void apostropheInPatternIsDoubled(String file) throws IOException {
        // in a MessageFormat pattern a single ' hides everything up to the next one, {0} included
        Properties texts = load(file);

        for (String key : texts.stringPropertyNames()) {
            String text = texts.getProperty(key);
            if (PLACEHOLDER.matcher(text).find()) {
                assertFalse(text.replace("''", "").contains("'"), key + " in " + file + " has a single apostrophe: " + text);
            }
        }
    }

    @Test
    void polishLettersAreReadAsUtf8() throws IOException {
        // ResourceBundle reads .properties as UTF-8 since Java 9; a file saved in another encoding breaks here
        assertEquals("Usuń z kolejki", Messages.load(java.util.Locale.of("pl")).getString("queue.remove"));
    }

    // ---------------------------------------------------------------- FXML

    @Test
    void fxmlHasNoTextOfItsOwn() throws IOException {
        // every text goes through the bundle: text="%toolbar.addFiles", never text="Dodaj pliki…"
        for (Path fxml : fxmlFiles()) {
            Matcher text = FXML_TEXT.matcher(Files.readString(fxml, StandardCharsets.UTF_8));
            while (text.find()) {
                assertTrue(text.group(2).startsWith("%"), fxml.getFileName() + " has its own text: " + text.group());
            }
        }
    }

    @Test
    void everyFxmlKeyIsTranslated() throws IOException {
        // FXMLLoader stops with an exception on a missing key, so a typo would break the window
        Properties polish = load("messages.properties");
        Properties english = load("messages_en.properties");

        for (Path fxml : fxmlFiles()) {
            Matcher text = FXML_TEXT.matcher(Files.readString(fxml, StandardCharsets.UTF_8));
            while (text.find()) {
                String key = text.group(2).substring(1);
                assertTrue(polish.containsKey(key), key + " from " + fxml.getFileName() + " missing in messages.properties");
                assertTrue(english.containsKey(key), key + " from " + fxml.getFileName() + " missing in messages_en.properties");
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    private static Set<String> withoutForms(Set<String> keys) {
        Set<String> names = new TreeSet<>();
        for (String key : keys) {
            Matcher counted = COUNTED.matcher(key);
            names.add(counted.matches() ? counted.group(1) : key);
        }
        return names;
    }

    private static Properties load(String file) throws IOException {
        try (InputStream in = Messages.class.getResourceAsStream("/pl/audiofix/i18n/" + file)) {
            assertNotNull(in, "no translation file " + file);
            Properties texts = new Properties();
            texts.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return texts;
        }
    }

    private static Set<String> placeholders(String text) {
        Set<String> found = new TreeSet<>();
        PLACEHOLDER.matcher(text).results().forEach(match -> found.add(match.group()));
        return found;
    }

    private static List<Path> fxmlFiles() throws IOException {
        try (Stream<Path> files = Files.list(FXML_FOLDER)) {
            List<Path> fxml = files.filter(file -> file.toString().endsWith(".fxml")).toList();
            assertFalse(fxml.isEmpty(), "no FXML files in " + FXML_FOLDER);
            return fxml;
        }
    }
}
