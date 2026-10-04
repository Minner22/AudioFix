package pl.audiofix.ffmpeg;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FfmpegLocatorTest {

    private static final String PREF_KEY = "ffmpegPath";

    @TempDir
    Path tempDir;

    private Preferences prefs;

    @BeforeEach
    void setUp() {
        prefs = Preferences.userRoot().node("audiofix-test/" + UUID.randomUUID());
    }

    @AfterEach
    void tearDown() throws BackingStoreException {
        prefs.removeNode();
    }

    // ---------------------------------------------------------------- locate: saved path

    @Test
    void savedPathWithBothExecutablesIsReturned() throws IOException {
        Path dir = ffmpegDir("saved", true);
        prefs.put(PREF_KEY, dir.resolve("ffmpeg.exe").toString());

        Optional<FfmpegPaths> result = locator(null).locate();

        assertEquals(Optional.of(paths(dir)), result);
    }

    @Test
    void savedPathIsPreferredOverPath() throws IOException {
        Path saved = ffmpegDir("saved", true);
        Path onPath = ffmpegDir("onPath", true);
        prefs.put(PREF_KEY, saved.resolve("ffmpeg.exe").toString());

        Optional<FfmpegPaths> result = locator(onPath.toString()).locate();

        assertEquals(Optional.of(paths(saved)), result);
    }

    @Test
    void missingSavedFileFallsBackToPathAndClearsPreference() throws IOException {
        Path onPath = ffmpegDir("onPath", true);
        prefs.put(PREF_KEY, tempDir.resolve("deleted/ffmpeg.exe").toString());

        Optional<FfmpegPaths> result = locator(onPath.toString()).locate();

        assertEquals(Optional.of(paths(onPath)), result);
        assertNull(prefs.get(PREF_KEY, null));
    }

    @Test
    void savedPathWithoutFfprobeIsNotReturned() throws IOException {
        Path dir = ffmpegDir("saved", false);
        prefs.put(PREF_KEY, dir.resolve("ffmpeg.exe").toString());

        assertEquals(Optional.empty(), locator(null).locate());
    }

    // ---------------------------------------------------------------- locate: PATH

    @Test
    void ffmpegIsFoundInLaterPathEntry() throws IOException {
        Path empty = Files.createDirectory(tempDir.resolve("empty"));
        Path onPath = ffmpegDir("onPath", true);

        Optional<FfmpegPaths> result = locator(pathOf(empty.toString(), onPath.toString())).locate();

        assertEquals(Optional.of(paths(onPath)), result);
    }

    @Test
    void quotedPathEntryIsHandled() throws IOException {
        Path onPath = ffmpegDir("with space", true);

        Optional<FfmpegPaths> result = locator("\"" + onPath + "\"").locate();

        assertEquals(Optional.of(paths(onPath)), result);
    }

    @Test
    void blankAndInvalidPathEntriesAreSkipped() throws IOException {
        Path onPath = ffmpegDir("onPath", true);

        Optional<FfmpegPaths> result = locator(pathOf("", "   ", "C:\\bad<>|path", onPath.toString())).locate();

        assertEquals(Optional.of(paths(onPath)), result);
    }

    @Test
    void nullPathReturnsEmpty() {
        assertEquals(Optional.empty(), locator(null).locate());
    }

    @Test
    void ffmpegWithoutFfprobeOnPathIsNotReturned() throws IOException {
        Path onPath = ffmpegDir("onPath", false);

        assertEquals(Optional.empty(), locator(onPath.toString()).locate());
    }

    @Test
    void directoryNamedFfmpegExeIsIgnored() throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("tricky"));
        Files.createDirectory(dir.resolve("ffmpeg.exe"));
        Files.createFile(dir.resolve("ffprobe.exe"));

        assertEquals(Optional.empty(), locator(dir.toString()).locate());
    }

    @Test
    void nothingFoundReturnsEmpty() throws IOException {
        Path empty = Files.createDirectory(tempDir.resolve("empty"));

        assertEquals(Optional.empty(), locator(empty.toString()).locate());
    }

    // ---------------------------------------------------------------- save

    @Test
    void saveStoresAbsolutePathAndReturnsBothExecutables() throws IOException {
        Path dir = ffmpegDir("chosen", true);
        Path ffmpeg = dir.resolve("ffmpeg.exe");

        FfmpegPaths result = locatorWithPassingVersionCheck().save(ffmpeg);

        assertEquals(paths(dir), result);
        assertEquals(ffmpeg.toAbsolutePath().toString(), prefs.get(PREF_KEY, null));
    }

    @Test
    void saveAcceptsUppercaseFileName() throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("upper"));
        Path ffmpeg = Files.createFile(dir.resolve("FFMPEG.EXE"));
        Files.createFile(dir.resolve("ffprobe.exe"));

        FfmpegPaths result = locatorWithPassingVersionCheck().save(ffmpeg);

        assertEquals(ffmpeg, result.ffmpeg());
    }

    @Test
    void savedPathIsFoundOnNextLocate() throws IOException {
        Path dir = ffmpegDir("chosen", true);
        locatorWithPassingVersionCheck().save(dir.resolve("ffmpeg.exe"));

        Optional<FfmpegPaths> result = locator(null).locate();

        assertEquals(Optional.of(paths(dir)), result);
    }

    @Test
    void saveRejectsFileWithOtherName() throws IOException {
        Path other = Files.createFile(tempDir.resolve("vlc.exe"));
        Files.createFile(tempDir.resolve("ffprobe.exe"));

        assertThrows(IllegalArgumentException.class, () -> locatorWithPassingVersionCheck().save(other));
        assertNull(prefs.get(PREF_KEY, null));
    }

    @Test
    void saveRejectsMissingFile() {
        Path missing = tempDir.resolve("ffmpeg.exe");

        assertThrows(IllegalArgumentException.class, () -> locatorWithPassingVersionCheck().save(missing));
    }

    @Test
    void saveRejectsFfmpegWithoutFfprobe() throws IOException {
        Path dir = ffmpegDir("noProbe", false);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> locatorWithPassingVersionCheck().save(dir.resolve("ffmpeg.exe")));
        assertTrue(ex.getMessage().contains("ffprobe"));
        assertNull(prefs.get(PREF_KEY, null));
    }

    @Test
    void saveRejectsFileThatIsNotRealFfmpeg() throws IOException {
        // empty ffmpeg.exe cannot be started, so the real version check must fail
        Path dir = ffmpegDir("fake", true);

        assertThrows(IllegalArgumentException.class, () -> locator(null).save(dir.resolve("ffmpeg.exe")));
        assertNull(prefs.get(PREF_KEY, null));
    }

    // ---------------------------------------------------------------- helpers

    private FfmpegLocator locator(String pathEnv) {
        return new FfmpegLocator(prefs, pathEnv);
    }

    private FfmpegLocator locatorWithPassingVersionCheck() {
        return new FfmpegLocator(prefs, null) {
            @Override
            boolean ffmpegVersionCheckOk(Path ffmpegPath) {
                return true;
            }
        };
    }

    private Path ffmpegDir(String name, boolean withFfprobe) throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve(name));
        Files.createFile(dir.resolve("ffmpeg.exe"));
        if (withFfprobe) {
            Files.createFile(dir.resolve("ffprobe.exe"));
        }
        return dir;
    }

    private static FfmpegPaths paths(Path dir) {
        return new FfmpegPaths(dir.resolve("ffmpeg.exe"), dir.resolve("ffprobe.exe"));
    }

    private static String pathOf(String... entries) {
        return String.join(File.pathSeparator, entries);
    }
}
