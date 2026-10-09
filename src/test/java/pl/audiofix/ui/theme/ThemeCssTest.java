package pl.audiofix.ui.theme;

import javafx.css.CssParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Rules of the style system read straight from the CSS files: base.css uses only -af-* variables,
 * every theme defines all of them.
 */
class ThemeCssTest {

    private static final Pattern VARIABLE = Pattern.compile("-af-[a-z0-9-]+");
    private static final Pattern DEFINITION = Pattern.compile("(-af-[a-z0-9-]+)\\s*:\\s*([^;]+);");
    private static final Pattern DECLARATION = Pattern.compile("([-a-z0-9]+)\\s*:\\s*[^;{}]+;");

    // ---------------------------------------------------------------- variables

    @Test
    void everyVariableUsedInBaseIsDefinedByEveryTheme() throws IOException {
        Set<String> used = used(css(Theme.BASE));

        for (Theme theme : Theme.values()) {
            Set<String> missing = new TreeSet<>(used);
            missing.removeAll(definitions(css(theme.fileName())).keySet());
            assertEquals(Set.of(), missing, theme.fileName() + " does not define variables used in base.css");
        }
    }

    @Test
    void everyThemeVariableIsUsedInBase() throws IOException {
        Set<String> used = used(css(Theme.BASE));

        for (Theme theme : Theme.values()) {
            Set<String> unused = new TreeSet<>(definitions(css(theme.fileName())).keySet());
            unused.removeAll(used);
            assertEquals(Set.of(), unused, theme.fileName() + " defines variables nobody uses");
        }
    }

    @Test
    void baseDefinesNoVariables() throws IOException {
        assertEquals(Map.of(), definitions(css(Theme.BASE)), "values belong to the theme files");
    }

    // ---------------------------------------------------------------- base.css: only rules

    @Test
    void baseHasNoColorLiterals() throws IOException {
        String base = css(Theme.BASE);

        Matcher literal = Pattern.compile("#[0-9a-fA-F]{3,8}\\b|\\b(rgb|rgba|hsb|hsba)\\s*\\(").matcher(base);
        if (literal.find()) {
            fail("color written directly in base.css: " + literal.group() + " - use an -af-* variable");
        }
    }

    @Test
    void baseHasNoGradientsOrShadows() throws IOException {
        String base = css(Theme.BASE);

        assertFalse(base.contains("gradient"), "flat design: no gradients");
        assertFalse(base.contains("dropshadow") || base.contains("innershadow"), "flat design: no shadows");
    }

    @Test
    void baseLeavesFontFamiliesToThemes() throws IOException {
        // JavaFX CSS cannot put a font family in a variable, so each theme sets its own
        assertFalse(css(Theme.BASE).contains("-fx-font-family"));
    }

    // ---------------------------------------------------------------- theme files: only values

    @Test
    void themesContainOnlyVariablesAndFontFamilies() throws IOException {
        for (Theme theme : Theme.values()) {
            Matcher declaration = DECLARATION.matcher(css(theme.fileName()));
            while (declaration.find()) {
                String property = declaration.group(1);
                assertTrue(property.startsWith("-af-") || property.equals("-fx-font-family"),
                        theme.fileName() + " should only hold values, found rule property " + property);
            }
        }
    }

    @Test
    void lightPaletteFollowsDesign() throws IOException {
        Map<String, String> light = definitions(css(Theme.LIGHT.fileName()));

        assertEquals("#f8fafc", light.get("-af-canvas"));
        assertEquals("#ffffff", light.get("-af-surface"));
        assertEquals("#cbd5e1", light.get("-af-border"));
        assertEquals("#0f172a", light.get("-af-text"));
        assertEquals("#0284c7", light.get("-af-accent"));
        assertEquals("#ea580c", light.get("-af-convert"));
        assertEquals("#15803d", light.get("-af-copy"));
    }

    @Test
    void lightThemeUsesWindowsFonts() throws IOException {
        String light = css(Theme.LIGHT.fileName());

        assertTrue(light.contains("\"Segoe UI\""), "UI font of Fluent Desktop Precision");
        assertTrue(light.contains("\"Consolas\""), "console font of Fluent Desktop Precision");
    }

    // ---------------------------------------------------------------- syntax

    @Test
    void stylesheetsParseWithoutErrors() {
        List<String> files = List.of(Theme.BASE, Theme.LIGHT.fileName());

        for (String file : files) {
            CssParser.errorsProperty().clear();
            assertNotNull(assertDoesNotThrow(() -> new CssParser().parse(Theme.class.getResource(file))));
            assertEquals(List.of(), List.copyOf(CssParser.errorsProperty()), file + " has CSS errors");
        }
    }

    // ---------------------------------------------------------------- helpers

    /** File content without comments, so commented-out code does not count. */
    static String css(String file) throws IOException {
        try (InputStream in = Theme.class.getResourceAsStream(file)) {
            assertNotNull(in, "no stylesheet " + file);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "");
        }
    }

    private static Set<String> used(String css) {
        Set<String> used = new TreeSet<>();
        VARIABLE.matcher(css).results().forEach(m -> used.add(m.group()));
        return used;
    }

    private static Map<String, String> definitions(String css) {
        Map<String, String> definitions = new LinkedHashMap<>();
        DEFINITION.matcher(css).results().forEach(m -> definitions.put(m.group(1), m.group(2).trim()));
        return definitions;
    }
}
