package pl.audiofix;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The log folder: LOCALAPPDATA on Windows, a folder in home elsewhere, the property above both. */
class LogFilesTest {

    private String saved;

    @BeforeEach
    void saveProperty() {
        saved = System.getProperty(LogFiles.PROPERTY);
        System.clearProperty(LogFiles.PROPERTY);
    }

    @AfterEach
    void restoreProperty() {
        if (saved == null) {
            System.clearProperty(LogFiles.PROPERTY);
        } else {
            System.setProperty(LogFiles.PROPERTY, saved);
        }
    }

    @Test
    void windowsLogsGoToLocalAppData() {
        Path dir = LogFiles.defaultDirectory(Map.of("LOCALAPPDATA", "C:\\Users\\Jan\\AppData\\Local"), "C:\\Users\\Jan");

        assertEquals(Path.of("C:\\Users\\Jan\\AppData\\Local", "AudioFix", "logs"), dir);
    }

    @Test
    void withoutLocalAppDataLogsGoToHome() {
        assertEquals(Path.of("/home/jan", ".audiofix", "logs"), LogFiles.defaultDirectory(Map.of(), "/home/jan"));
        assertEquals(Path.of("/home/jan", ".audiofix", "logs"), LogFiles.defaultDirectory(Map.of("LOCALAPPDATA", " "), "/home/jan"));
    }

    @Test
    void propertyWins() {
        System.setProperty(LogFiles.PROPERTY, "D:\\logi");

        assertEquals(Path.of("D:\\logi"), LogFiles.directory());
    }

    @Test
    void configureFixesFolderForLogback() {
        LogFiles.configure();

        assertEquals(LogFiles.defaultDirectory(System.getenv(), System.getProperty("user.home")).toString(),
                System.getProperty(LogFiles.PROPERTY));
    }

    @Test
    void configureKeepsFolderGivenOnCommandLine() {
        System.setProperty(LogFiles.PROPERTY, "D:\\logi");

        LogFiles.configure();

        assertEquals("D:\\logi", System.getProperty(LogFiles.PROPERTY));
    }
}
