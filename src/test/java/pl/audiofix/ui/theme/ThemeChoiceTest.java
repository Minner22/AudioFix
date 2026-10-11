package pl.audiofix.ui.theme;

import javafx.application.ColorScheme;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Which theme each menu choice means, without any UI. */
class ThemeChoiceTest {

    @ParameterizedTest(name = "{0} + Windows {1} -> {2}")
    @CsvSource({
            "SYSTEM, LIGHT, LIGHT",
            "SYSTEM, DARK,  DARK",
            "LIGHT,  LIGHT, LIGHT",
            "LIGHT,  DARK,  LIGHT",
            "DARK,   LIGHT, DARK",
            "DARK,   DARK,  DARK",
    })
    void choiceResolvesAgainstWindowsScheme(ThemeChoice choice, ColorScheme system, Theme expected) {
        assertEquals(expected, choice.resolve(system));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SYSTEM", "LIGHT", "DARK"})
    void savedNameIsReadBack(String saved) {
        assertEquals(ThemeChoice.valueOf(saved), ThemeChoice.parse(saved));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"dark", "BLUE", " DARK"})
    void unknownSavedValueMeansSystem(String saved) {
        // an old version or a hand-edited registry must not stop the app from starting
        assertEquals(ThemeChoice.SYSTEM, ThemeChoice.parse(saved));
    }
}
