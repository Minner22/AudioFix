package pl.audiofix.ui.theme;

import javafx.application.Platform;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Geist and JetBrains Mono (the dark design's fonts) are bundled, because Windows does not have them. */
class ThemeFontsTest {

    private static List<Font> loaded;

    @BeforeAll
    static void loadFonts() {
        CompletableFuture<Void> started = new CompletableFuture<>();
        try {
            Platform.startup(() -> started.complete(null));
        } catch (IllegalStateException alreadyStarted) {
            started.complete(null);
        }
        Platform.setImplicitExit(false);
        started.orTimeout(10, TimeUnit.SECONDS).join();

        loaded = ThemeFonts.load();
    }

    @Test
    void allBundledFontsLoad() {
        assertEquals(ThemeFonts.FILES.size(), loaded.size(), "loaded: " + loaded);
    }

    @Test
    void familiesAreAvailableToCss() {
        List<String> families = Font.getFamilies();

        assertTrue(families.contains("Geist"), "Geist missing");
        assertTrue(families.contains("JetBrains Mono"), "JetBrains Mono missing");
    }

    @Test
    void boldGeistIsTheBundledBoldFace() {
        // -fx-font-weight: bold must find "Geist Bold"; a SemiBold file would register as a separate family
        Font bold = Font.font("Geist", FontWeight.BOLD, 12);

        assertEquals("Geist", bold.getFamily());
        assertEquals("Geist Bold", bold.getName());
    }

    @Test
    void loadingTwiceIsHarmless() {
        assertEquals(ThemeFonts.FILES.size(), ThemeFonts.load().size());
    }

    @Test
    void licenseIsShippedWithEveryFont() {
        // SIL OFL 1.1: the license text has to be distributed together with the font files
        assertNotNull(ThemeFonts.class.getResource("fonts/OFL-Geist.txt"));
        assertNotNull(ThemeFonts.class.getResource("fonts/OFL-JetBrainsMono.txt"));
    }

    @Test
    void everyListedFileIsOnClasspath() {
        for (String file : ThemeFonts.FILES) {
            assertNotNull(ThemeFonts.class.getResource("fonts/" + file), file);
        }
    }
}
