package pl.audiofix.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The language for a first start and the remembered PL | EN choice (#31), with an isolated Preferences node. */
class LanguageSettingsTest {

    private Preferences prefs;

    @BeforeEach
    void isolatedNode() {
        prefs = Preferences.userRoot().node("audiofix-test/" + UUID.randomUUID());
    }

    @AfterEach
    void removeNode() throws BackingStoreException {
        prefs.removeNode();
    }

    // ---------------------------------------------------------------- Language

    @Test
    void polishSystemStartsInPolish() {
        assertEquals(Language.POLISH, Language.forSystem(Locale.of("pl", "PL")));
        assertEquals(Language.POLISH, Language.forSystem(Locale.of("pl")));
    }

    @Test
    void anyOtherSystemStartsInEnglish() {
        assertEquals(Language.ENGLISH, Language.forSystem(Locale.US));
        assertEquals(Language.ENGLISH, Language.forSystem(Locale.GERMANY));
        assertEquals(Language.ENGLISH, Language.forSystem(Locale.ROOT));
    }

    @Test
    void languagesHaveTheLocalesOfTheirBundles() {
        assertEquals("pl", Language.POLISH.locale().getLanguage());
        assertEquals("en", Language.ENGLISH.locale().getLanguage());
    }

    @Test
    void savedValueIsParsed() {
        assertEquals(Language.ENGLISH, Language.parse("ENGLISH").orElseThrow());
        assertEquals(Language.POLISH, Language.parse("POLISH").orElseThrow());
        assertEquals(java.util.Optional.empty(), Language.parse(null));
        assertEquals(java.util.Optional.empty(), Language.parse("KLINGON"));
        assertEquals(java.util.Optional.empty(), Language.parse("english"));
    }

    // ---------------------------------------------------------------- LanguageSettings

    @Test
    void withNothingSavedTheSystemLanguageIsUsedAndNotSaved() {
        assertEquals(Language.POLISH, new LanguageSettings(prefs, Locale.of("pl", "PL")).getLanguage());
        assertEquals(Language.ENGLISH, new LanguageSettings(prefs, Locale.GERMANY).getLanguage());
        assertNull(prefs.get(LanguageSettings.KEY, null), "nothing to save until the user picks a language");
    }

    @Test
    void savedChoiceWinsOverTheSystem() {
        prefs.put(LanguageSettings.KEY, "ENGLISH");

        assertEquals(Language.ENGLISH, new LanguageSettings(prefs, Locale.of("pl", "PL")).getLanguage());
    }

    @Test
    void unknownSavedValueFallsBackToTheSystem() {
        prefs.put(LanguageSettings.KEY, "KLINGON");

        assertEquals(Language.POLISH, new LanguageSettings(prefs, Locale.of("pl", "PL")).getLanguage());
    }

    @Test
    void choiceIsSavedAndReadOnTheNextStart() {
        LanguageSettings settings = new LanguageSettings(prefs, Locale.of("pl", "PL"));

        settings.setLanguage(Language.ENGLISH);

        assertEquals("ENGLISH", prefs.get(LanguageSettings.KEY, null));
        assertEquals(Language.ENGLISH, settings.languageProperty().get());
        assertEquals(Language.ENGLISH, new LanguageSettings(prefs, Locale.of("pl", "PL")).getLanguage());
    }
}
