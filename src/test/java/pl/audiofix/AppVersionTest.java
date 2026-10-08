package pl.audiofix;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppVersionTest {

    private static final String SEMVER = "\\d+\\.\\d+\\.\\d+(-SNAPSHOT)?";

    @Test
    void versionIsNotBlank() {
        assertFalse(AppVersion.current().isBlank());
    }

    @Test
    void versionHasNoUnresolvedPlaceholder() {
        // Maven resource filtering must replace ${project.version}
        assertFalse(AppVersion.current().contains("${"), AppVersion.current());
    }

    @Test
    void versionIsSemVer() {
        assertTrue(AppVersion.current().matches(SEMVER),
                "expected X.Y.Z or X.Y.Z-SNAPSHOT, was: " + AppVersion.current());
    }

    @Test
    void versionMatchesPom() {
        // surefire passes <version> from pom.xml as the system property project.version
        String fromPom = System.getProperty("project.version");
        assertNotNull(fromPom, "system property project.version not set - check surefire configuration in pom.xml");

        assertEquals(fromPom, AppVersion.current());
    }

    @Test
    void versionIsReadOnce() {
        assertSame(AppVersion.current(), AppVersion.current());
    }

    @Test
    void filteredResourceIsOnClasspath() throws IOException {
        try (InputStream in = AppVersion.class.getResourceAsStream("version.properties")) {
            assertNotNull(in, "pl/audiofix/version.properties missing");
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(content.contains("version=" + System.getProperty("project.version")), content);
        }
    }

    @Test
    void otherResourcesAreStillCopied() throws IOException {
        // the second <resource> block (without filtering) copies FXML, CSS and logback.xml - without it they disappear
        try (InputStream in = AudioFixApp.class.getResourceAsStream("ui/main-view.fxml")) {
            assertNotNull(in);
            String fxml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(fxml.contains("fx:controller=\"pl.audiofix.ui.MainController\""));
        }
    }

    @Test
    void windowTitleContainsVersion() {
        assertEquals("AudioFix " + AppVersion.current(), AudioFixApp.windowTitle());
    }
}
