package pl.audiofix.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Texts for the user come only from the translation files: no string literal in the UI (#29), the model,
 * ffmpeg or the queue (#30) may hold Polish letters or common Polish words. Comments and Javadoc do not count.
 */
class NoHardcodedTextsTest {

    private static final Path SOURCES = Path.of("src", "main", "java", "pl", "audiofix");
    private static final List<String> PACKAGES = List.of("ui", "model", "ffmpeg", "queue");

    /** Files allowed to keep Polish words; empty since #30. */
    private static final Set<String> ALLOWED = Set.of();

    private static final Pattern LITERAL = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern POLISH = Pattern.compile(
            "[ąćęłńóśźżĄĆĘŁŃÓŚŹŻ„”]|\\b(Oczekuje|Gotowe|Anulowano|Wideo|Napisy|Inne|Kopiuj|Wybierz|Filmy|Zapisz|Start konwersji)\\b");

    @Test
    void codeHasNoPolishTexts() throws IOException {
        List<String> found = new ArrayList<>();
        for (String folder : PACKAGES) {
            found.addAll(polishTextsIn(SOURCES.resolve(folder)));
        }

        assertEquals(List.of(), found, "texts for the user belong in messages.properties");
    }

    private static List<String> polishTextsIn(Path folder) throws IOException {
        List<String> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(folder)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                if (ALLOWED.contains(file.getFileName().toString())) {
                    continue;
                }
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    String code = withoutComment(lines.get(i));
                    Matcher literal = LITERAL.matcher(code);
                    while (literal.find()) {
                        if (POLISH.matcher(literal.group(1)).find()) {
                            found.add(file.getFileName() + ":" + (i + 1) + " " + literal.group());
                        }
                    }
                }
            }
        }
        return found;
    }

    @Test
    void checkFindsPolishTexts() {
        // the test above is only as good as its patterns
        assertEquals(true, POLISH.matcher("Nie można odczytać plików").find());
        assertEquals(true, POLISH.matcher("Oczekuje").find());
        assertEquals(true, POLISH.matcher("Wideo").find());
        assertEquals(false, POLISH.matcher("track.type.VIDEO").find());
        assertEquals("code ", withoutComment("code // a comment with \"Gotowe\""));
        assertEquals("", withoutComment("     * Javadoc line with \"Błąd\""));
    }

    /** The line without a // comment, or empty for a Javadoc / block comment line. */
    private static String withoutComment(String line) {
        String trimmed = line.strip();
        if (trimmed.startsWith("*") || trimmed.startsWith("/*")) {
            return "";
        }
        boolean inString = false;
        for (int i = 0; i < line.length() - 1; i++) {
            char c = line.charAt(i);
            if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) {
                inString = !inString;
            } else if (!inString && c == '/' && line.charAt(i + 1) == '/') {
                return line.substring(0, i);
            }
        }
        return line;
    }
}
