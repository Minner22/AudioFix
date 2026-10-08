package pl.audiofix.ui;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AboutInfoTest {

    private static final Path FFMPEG = Path.of("C:\\ffmpeg\\bin\\ffmpeg.exe");

    @Test
    void clipboardTextHasOneLinePerItem() {
        AboutInfo info = new AboutInfo("0.2.0", FFMPEG, "ffmpeg version 7.0.1", "25.0.2", "25.0.4", "Windows 11 10.0");

        List<String> lines = info.toClipboardText().lines().toList();

        assertEquals(List.of(
                "AudioFix 0.2.0",
                "ffmpeg: ffmpeg version 7.0.1",
                "ffmpeg path: C:\\ffmpeg\\bin\\ffmpeg.exe",
                "Java: 25.0.2",
                "JavaFX: 25.0.4",
                "OS: Windows 11 10.0"), lines);
    }

    @Test
    void unknownFfmpegVersionIsMarked() {
        AboutInfo info = new AboutInfo("0.2.0", FFMPEG, null, "25", "25", "Windows");

        assertTrue(info.toClipboardText().contains("ffmpeg: nieznana"), info.toClipboardText());
    }

    @Test
    void missingFfmpegPathIsMarked() {
        AboutInfo info = new AboutInfo("0.2.0", null, null, "25", "25", "Windows");

        assertTrue(info.toClipboardText().contains("ffmpeg path: nieznana"), info.toClipboardText());
    }

    @Test
    void withFfmpegVersionChangesOnlyTheVersion() {
        AboutInfo pending = new AboutInfo("0.2.0", FFMPEG, null, "25.0.2", "25.0.4", "Windows 11 10.0");

        AboutInfo known = pending.withFfmpegVersion("ffmpeg version 7.0.1");

        assertEquals(new AboutInfo("0.2.0", FFMPEG, "ffmpeg version 7.0.1", "25.0.2", "25.0.4", "Windows 11 10.0"), known);
    }

    @Test
    void ofFillsEnvironmentFromRunningJvm() {
        FxTestSupport.startJavaFx();   // JavaFX sets javafx.runtime.version when it starts

        AboutInfo info = AboutInfo.of("0.2.0", FFMPEG, "ffmpeg version 7.0.1");

        assertEquals(Runtime.version().toString(), info.javaVersion());
        assertTrue(info.os().startsWith(System.getProperty("os.name")), info.os());
        assertTrue(info.javafxVersion().matches("\\d+.*"), "expected JavaFX version, was: " + info.javafxVersion());
    }
}
