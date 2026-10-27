package pl.audiofix.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pl.audiofix.model.MediaInfo;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextFormatsTest {

    // ---------------------------------------------------------------- file size

    @ParameterizedTest(name = "{0} B -> \"{1}\"")
    @CsvSource(delimiter = '|', textBlock = """
            0              | 0 B
            512            | 512 B
            1023           | 1023 B
            1024           | 1,0 KB
            1536           | 1,5 KB
            5242880        | 5,0 MB
            1503238553     | 1,4 GB
            34789235712    | 32,4 GB
            2199023255552  | 2,0 TB
            """)
    void sizeLikeWindowsExplorer(long bytes, String expected) {
        assertEquals(expected, TextFormats.fileSize(bytes));
    }

    @ParameterizedTest(name = "{0} B -> \"{1}\" in English")
    @CsvSource(delimiter = '|', textBlock = """
            512            | 512 B
            1536           | 1.5 KB
            1503238553     | 1.4 GB
            34789235712    | 32.4 GB
            """)
    void sizeHasTheDecimalPointOfTheLanguage(long bytes, String expected) {
        inEnglish(() -> assertEquals(expected, TextFormats.fileSize(bytes)));
    }

    @Test
    void sizeFollowsALanguageSwitch() {
        inEnglish(() -> assertEquals("1.5 KB", TextFormats.fileSize(1536)));
        assertEquals("1,5 KB", TextFormats.fileSize(1536), "back in Polish");
    }

    // ---------------------------------------------------------------- Polish plural

    @ParameterizedTest(name = "{0} -> \"{1}\"")
    @CsvSource(delimiter = '|', textBlock = """
            0    | 0 ścieżek
            1    | 1 ścieżka
            2    | 2 ścieżki
            4    | 4 ścieżki
            5    | 5 ścieżek
            9    | 9 ścieżek
            11   | 11 ścieżek
            12   | 12 ścieżek
            14   | 14 ścieżek
            21   | 21 ścieżek
            22   | 22 ścieżki
            24   | 24 ścieżki
            25   | 25 ścieżek
            101  | 101 ścieżek
            112  | 112 ścieżek
            122  | 122 ścieżki
            """)
    void countUsesPolishNounForms(long n, String expected) {
        // tests run in Polish (pom.xml); the forms come from messages.properties
        assertEquals(expected, Messages.count("tracks.count", n));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "0,    many",
            "1,    one",
            "2,    few",
            "4,    few",
            "5,    many",
            "11,   many",
            "12,   many",
            "13,   many",
            "14,   many",
            "21,   many",
            "22,   few",
            "24,   few",
            "25,   many",
            "101,  many",
            "112,  many",
            "122,  few",
            "1002, few",
    })
    void polishPluralForm(long n, String form) {
        assertEquals(form, TextFormats.pluralForm(Locale.of("pl"), n));
        assertEquals(form, TextFormats.pluralForm(Locale.of("pl", "PL"), n), "the country does not matter");
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "0,    many",
            "1,    one",
            "2,    many",
            "4,    many",
            "21,   many",
            "22,   many",
            "101,  many",
    })
    void englishPluralForm(long n, String form) {
        assertEquals(form, TextFormats.pluralForm(Locale.ENGLISH, n));
        assertEquals(form, TextFormats.pluralForm(Locale.US, n));
    }

    @Test
    void formsOfEachLanguage() {
        assertEquals(Set.of("one", "few", "many"), TextFormats.pluralForms(Locale.of("pl")));
        assertEquals(Set.of("one", "many"), TextFormats.pluralForms(Locale.ENGLISH));
    }

    // ---------------------------------------------------------------- English plural (#32)

    @ParameterizedTest(name = "{0} -> \"{1}\"")
    @CsvSource(delimiter = '|', textBlock = """
            0    | 0 tracks
            1    | 1 track
            2    | 2 tracks
            3    | 3 tracks
            22   | 22 tracks
            25   | 25 tracks
            """)
    void countUsesEnglishNounForms(long n, String expected) {
        inEnglish(() -> assertEquals(expected, Messages.count("tracks.count", n)));
    }

    @ParameterizedTest(name = "{0} tracks")
    @CsvSource(delimiter = '|', textBlock = """
            1  | Wczytano Drive.mkv: 1 ścieżka  | Loaded Drive.mkv: 1 track
            3  | Wczytano Drive.mkv: 3 ścieżki  | Loaded Drive.mkv: 3 tracks
            22 | Wczytano Drive.mkv: 22 ścieżki | Loaded Drive.mkv: 22 tracks
            25 | Wczytano Drive.mkv: 25 ścieżek | Loaded Drive.mkv: 25 tracks
            """)
    void loadedLineInBothLanguages(long n, String polish, String english) {
        assertEquals(polish, Messages.get("log.loaded", "Drive.mkv", Messages.count("tracks.count", n)));
        inEnglish(() -> assertEquals(english, Messages.get("log.loaded", "Drive.mkv", Messages.count("tracks.count", n))));
    }

    // ---------------------------------------------------------------- container

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource(delimiter = '|', textBlock = """
            Matroska / WebM                     | Matroska
            QuickTime / MOV                     | QuickTime
            MPEG-TS (MPEG-2 Transport Stream)   | MPEG-TS
            AVI (Audio Video Interleaved)       | AVI
            raw H.264 video                     | raw H.264 video
            """)
    void containerNameIsShortPartOfFfprobeName(String formatLongName, String expected) {
        assertEquals(expected, TextFormats.containerName(formatLongName));
    }

    @Test
    void unknownContainerIsEmpty() {
        assertEquals("", TextFormats.containerName(null));
        assertEquals("", TextFormats.containerName(""));
    }

    // ---------------------------------------------------------------- tracks header

    @Test
    void fileInfoHasSizeAndContainer() {
        assertEquals("(32,4 GB, Matroska)", TextFormats.fileInfo(media(34_789_235_712L, "Matroska / WebM")));
    }

    @Test
    void fileInfoShowsOnlyWhatIsKnown() {
        assertEquals("(32,4 GB)", TextFormats.fileInfo(media(34_789_235_712L, null)));
        assertEquals("(Matroska)", TextFormats.fileInfo(media(0, "Matroska / WebM")));
        assertEquals("", TextFormats.fileInfo(media(0, null)), "no empty brackets");
    }

    // ---------------------------------------------------------------- duration (#46)

    @ParameterizedTest(name = "{0} s -> {1}")
    @CsvSource({
            "0,     00:00",
            "59,    00:59",
            "135,   02:15",
            "3599,  59:59",
            "3600,  1:00:00",
            "3725,  1:02:05",
            "36000, 10:00:00",
    })
    void durationIsMinutesAndSecondsOrHours(long seconds, String expected) {
        assertEquals(expected, TextFormats.duration(java.time.Duration.ofSeconds(seconds)));
    }

    @Test
    void durationIsRoundedToWholeSeconds() {
        assertEquals("01:00", TextFormats.duration(java.time.Duration.ofMillis(59_600)));
        assertEquals("00:59", TextFormats.duration(java.time.Duration.ofMillis(59_400)));
    }

    private static void inEnglish(Runnable check) {
        Messages.use(Locale.ENGLISH);
        try {
            check.run();
        } finally {
            Messages.use(Locale.getDefault());
        }
    }

    private static MediaInfo media(long size, String formatName) {
        return new MediaInfo(Path.of("Drive.mkv"), 60, List.of(), size, formatName);
    }
}
