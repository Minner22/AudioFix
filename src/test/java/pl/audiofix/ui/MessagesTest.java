package pl.audiofix.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Loading the bundle for a language and formatting texts with arguments. */
class MessagesTest {

    private static final Locale POLISH = Locale.of("pl");

    private final Locale savedDefault = Locale.getDefault();

    @AfterEach
    void restore() {
        Locale.setDefault(savedDefault);
        Messages.use(savedDefault);
    }

    @Test
    void polishIsTheDefaultBundle() {
        assertEquals("Dodaj pliki…", Messages.load(POLISH).getString("toolbar.addFiles"));
    }

    @Test
    void englishHasItsOwnBundle() {
        assertEquals("Add files…", Messages.load(Locale.ENGLISH).getString("toolbar.addFiles"));
        assertEquals("Add files…", Messages.load(Locale.UK).getString("toolbar.addFiles"), "any English country");
    }

    @Test
    void polishStaysPolishOnEnglishWindows() {
        // the usual fallback would go from the missing messages_pl to the system language, messages_en
        Locale.setDefault(Locale.US);

        assertEquals("Dodaj pliki…", Messages.load(Locale.of("pl", "PL")).getString("toolbar.addFiles"));
    }

    @Test
    void useSwitchesTheLanguage() {
        Messages.use(Locale.ENGLISH);
        assertEquals("Cancel", Messages.get("actions.cancel"));
        assertEquals(Locale.ENGLISH, Messages.locale());

        Messages.use(POLISH);
        assertEquals("Anuluj", Messages.get("actions.cancel"));
        assertEquals(POLISH, Messages.locale());
    }

    @Test
    void countInEnglishHasOneAndOther() {
        Messages.use(Locale.ENGLISH);

        assertEquals("0 tracks", Messages.count("tracks.count", 0));
        assertEquals("1 track", Messages.count("tracks.count", 1));
        assertEquals("3 tracks", Messages.count("tracks.count", 3));
        assertEquals("22 tracks", Messages.count("tracks.count", 22), "the Polish 'few' is plural in English too");
    }

    @Test
    void textWithoutArgumentsIsTakenAsItIs() {
        // FXMLLoader shows texts as they are, so get() without arguments must not treat ' or {} specially
        assertEquals("it's {not} a pattern", Messages.format(bundle("it's {not} a pattern"), POLISH, "text"));
    }

    @Test
    void argumentsFillThePattern() {
        assertEquals("Wczytano Drive.mkv: 9", Messages.format(bundle("Wczytano {0}: {1}"), POLISH, "text", "Drive.mkv", 9));
    }

    @Test
    void doubledApostropheInPatternIsOne() {
        assertEquals("Can't read Drive.mkv", Messages.format(bundle("Can''t read {0}"), Locale.ENGLISH, "text", "Drive.mkv"));
    }

    @Test
    void numbersFollowTheAskedLanguage() {
        // the Polish texts are the base bundle: its own locale is the empty root one, which writes 1,234.5
        assertEquals(Locale.ROOT, Messages.load(POLISH).getLocale());

        ResourceBundle base = new ListResourceBundle() {
            @Override
            protected Object[][] getContents() {
                return new Object[][] {{"text", "{0}"}};
            }

            @Override
            public Locale getLocale() {
                return Locale.ROOT;   // like the real messages.properties
            }
        };
        String polish = Messages.format(base, POLISH, "text", 1234.5);
        String english = Messages.format(base, Locale.ENGLISH, "text", 1234.5);

        assertEquals("1 234,5", polish.replace(' ', ' ').replace(' ', ' '));
        assertEquals("1,234.5", english);
    }

    private static ResourceBundle bundle(String text) {
        return new ListResourceBundle() {
            @Override
            protected Object[][] getContents() {
                return new Object[][] {{"text", text}};
            }
        };
    }
}
