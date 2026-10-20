package pl.audiofix.ffmpeg;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.audiofix.model.AudioFixException;
import pl.audiofix.model.UserMessage;

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
    void invalidSavedPathFallsBackToPathAndClearsPreference() throws IOException {
        Path onPath = ffmpegDir("onPath", true);
        prefs.put(PREF_KEY, "C:\\bad<>|path\\ffmpeg.exe");

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

    // ---------------------------------------------------------------- locate: shipped with the installer (#24)

    @Test
    void bundledFfmpegIsUsedWhenNothingIsSaved() throws IOException {
        Path bundled = ffmpegDir("bundled", true);
        Path onPath = ffmpegDir("onPath", true);

        Optional<FfmpegPaths> result = new FfmpegLocator(prefs, bundled, onPath.toString()).locate();

        assertEquals(Optional.of(paths(bundled)), result, "the shipped copy goes before PATH");
    }

    @Test
    void savedPathWinsOverBundledFfmpeg() throws IOException {
        // the user chose e.g. a newer ffmpeg on purpose
        Path saved = ffmpegDir("saved", true);
        Path bundled = ffmpegDir("bundled", true);
        prefs.put(PREF_KEY, saved.resolve("ffmpeg.exe").toString());

        Optional<FfmpegPaths> result = new FfmpegLocator(prefs, bundled, null).locate();

        assertEquals(Optional.of(paths(saved)), result);
    }

    @Test
    void incompleteBundledFfmpegFallsBackToPath() throws IOException {
        Path bundled = ffmpegDir("bundled", false);   // ffprobe.exe missing, e.g. removed by an antivirus
        Path onPath = ffmpegDir("onPath", true);

        Optional<FfmpegPaths> result = new FfmpegLocator(prefs, bundled, onPath.toString()).locate();

        assertEquals(Optional.of(paths(onPath)), result);
    }

    @Test
    void missingBundledFolderFallsBackToPath() throws IOException {
        Path onPath = ffmpegDir("onPath", true);

        Optional<FfmpegPaths> result = new FfmpegLocator(prefs, tempDir.resolve("no-such-folder"), onPath.toString()).locate();

        assertEquals(Optional.of(paths(onPath)), result);
    }

    @Test
    void bundledFfmpegIsNeverSaved() throws IOException {
        // saving it would pin this installation's path; a later version may install elsewhere
        new FfmpegLocator(prefs, ffmpegDir("bundled", true), null).locate();

        assertNull(prefs.get(PREF_KEY, null));
    }

    @Test
    void bundledFolderIsNextToAppJars() {
        Path folder = FfmpegLocator.bundledFolder("C:\\Users\\Jan\\AppData\\Local\\AudioFix\\AudioFix.exe").orElseThrow();

        assertEquals(Path.of("C:\\Users\\Jan\\AppData\\Local\\AudioFix", "app", "ffmpeg"), folder);
    }

    @Test
    void withoutInstalledExeThereIsNoBundledFolder() {
        // started from the IDE or with mvn javafx:run: jpackage.app-path is not set
        assertEquals(Optional.empty(), FfmpegLocator.bundledFolder(null));
        assertEquals(Optional.empty(), FfmpegLocator.bundledFolder(" "));
        assertEquals(Optional.empty(), FfmpegLocator.bundledFolder("AudioFix.exe"), "no folder to look in");
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

        var ex = assertThrows(AudioFixException.class, () -> locatorWithPassingVersionCheck().save(other));
        assertEquals(UserMessage.of("ffmpeg.invalid.notFfmpeg", other), ex.userMessage());
        assertNull(prefs.get(PREF_KEY, null));
    }

    @Test
    void saveRejectsMissingFile() {
        Path missing = tempDir.resolve("ffmpeg.exe");

        var ex = assertThrows(AudioFixException.class, () -> locatorWithPassingVersionCheck().save(missing));
        assertEquals("ffmpeg.invalid.notFfmpeg", ex.userMessage().key());
    }

    @Test
    void saveRejectsFfmpegWithoutFfprobe() throws IOException {
        Path dir = ffmpegDir("noProbe", false);

        var ex = assertThrows(AudioFixException.class,
                () -> locatorWithPassingVersionCheck().save(dir.resolve("ffmpeg.exe")));
        assertEquals(UserMessage.of("ffmpeg.invalid.noFfprobe", dir), ex.userMessage());
        assertTrue(ex.getMessage().contains("ffprobe"), "English message for the log: " + ex.getMessage());
        assertNull(prefs.get(PREF_KEY, null));
    }

    @Test
    void saveRejectsFileThatIsNotRealFfmpeg() throws IOException {
        // empty ffmpeg.exe cannot be started, so the real version check must fail
        Path dir = ffmpegDir("fake", true);

        var ex = assertThrows(AudioFixException.class, () -> locator(null).save(dir.resolve("ffmpeg.exe")));
        assertEquals("ffmpeg.invalid.doesNotRun", ex.userMessage().key());
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
