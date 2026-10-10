package pl.audiofix.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DroppedFilesTest {

    @TempDir
    Path dir;

    // ---------------------------------------------------------------- extensions

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(textBlock = """
            Film.mkv,                 true
            Film.MKV,                 true
            Film.mp4,                 true
            Film.m2ts,                true
            Film.M2TS,                true
            Drive.2011.REMUX.mkv,     true
            Film.avi,                 false
            napisy.srt,               false
            notatki.txt,              false
            mkv,                      false
            Film.mkv.part,            false
            """)
    void supportedExtensionsIgnoreCase(String name, boolean supported) {
        assertEquals(supported, DroppedFiles.isSupported(dir.resolve(name)));
    }

    @Test
    void chooserUsesTheSameExtensions() {
        assertEquals(List.of("*.mkv", "*.mp4", "*.m2ts"), DroppedFiles.chooserPatterns());
    }

    // ---------------------------------------------------------------- dropped files

    @Test
    void filmsAreKeptInDropOrderAndOthersSkipped() throws IOException {
        Path b = file("B.mkv");
        Path txt = file("notatki.txt");
        Path a = file("A.mp4");

        DroppedFiles files = DroppedFiles.of(List.of(b, txt, a));

        assertEquals(List.of(b, a), files.supported());
        assertEquals(List.of(txt), files.skipped());
    }

    @Test
    void onlyUnsupportedFilesGiveNothingToAdd() throws IOException {
        DroppedFiles files = DroppedFiles.of(List.of(file("a.txt"), file("b.srt")));

        assertEquals(List.of(), files.supported());
        assertEquals(2, files.skipped().size());
    }

    @Test
    void sameFileDroppedTwiceIsAddedOnce() throws IOException {
        Path film = file("Film.mkv");

        assertEquals(List.of(film), DroppedFiles.of(List.of(film, film)).supported());
    }

    // ---------------------------------------------------------------- folders

    @Test
    void folderGivesItsFilmsSortedByName() throws IOException {
        Path season = Files.createDirectory(dir.resolve("Sezon 1"));
        Path e2 = Files.createFile(season.resolve("Serial S01E02.mkv"));
        Path e10 = Files.createFile(season.resolve("serial S01E10.mkv"));
        Path e1 = Files.createFile(season.resolve("Serial S01E01.mkv"));
        Files.createFile(season.resolve("Serial S01E01.srt"));
        Files.createFile(season.resolve("okladka.jpg"));

        DroppedFiles files = DroppedFiles.of(List.of(season));

        assertEquals(List.of(e1, e2, e10), files.supported(), "sorted by name, ignoring case");
        assertEquals(List.of(), files.skipped(), "other files inside a folder are not reported one by one");
    }

    @Test
    void subfoldersAreNotSearched() throws IOException {
        Path season = Files.createDirectory(dir.resolve("Sezon 1"));
        Path extras = Files.createDirectory(season.resolve("Dodatki"));
        Files.createFile(extras.resolve("Making of.mkv"));
        Path episode = Files.createFile(season.resolve("Odcinek 1.mkv"));

        assertEquals(List.of(episode), DroppedFiles.of(List.of(season)).supported());
    }

    @Test
    void folderWithoutFilmsIsSkipped() throws IOException {
        Path empty = Files.createDirectory(dir.resolve("Zdjęcia"));
        Files.createFile(empty.resolve("plaża.jpg"));

        DroppedFiles files = DroppedFiles.of(List.of(empty));

        assertEquals(List.of(), files.supported());
        assertEquals(List.of(empty), files.skipped());
    }

    @Test
    void folderNamedLikeFilmIsTreatedAsFolder() throws IOException {
        Path folder = Files.createDirectory(dir.resolve("Film.mkv"));
        Path inside = Files.createFile(folder.resolve("Film.mkv"));

        assertEquals(List.of(inside), DroppedFiles.of(List.of(folder)).supported());
    }

    @Test
    void filmsAndFoldersCanBeMixed() throws IOException {
        Path single = file("Film.mkv");
        Path season = Files.createDirectory(dir.resolve("Sezon"));
        Path episode = Files.createFile(season.resolve("Odcinek 1.mkv"));

        assertEquals(List.of(single, episode), DroppedFiles.of(List.of(single, season)).supported());
    }

    // ---------------------------------------------------------------- while dragging

    @Test
    void dragWithFilmOrFolderMayBeAccepted() throws IOException {
        assertTrue(DroppedFiles.mayContainFilms(List.of(file("a.txt"), file("Film.mkv"))));
        assertTrue(DroppedFiles.mayContainFilms(List.of(Files.createDirectory(dir.resolve("Sezon")))));
    }

    @Test
    void dragWithOnlyOtherFilesIsRejected() throws IOException {
        assertFalse(DroppedFiles.mayContainFilms(List.of(file("a.txt"), file("b.srt"))));
        assertFalse(DroppedFiles.mayContainFilms(List.of()));
    }

    @Test
    void listsCannotBeChanged() throws IOException {
        DroppedFiles files = DroppedFiles.of(List.of(file("Film.mkv")));

        assertThrows(UnsupportedOperationException.class, () -> files.supported().clear());
    }

    // ---------------------------------------------------------------- helpers

    private Path file(String name) throws IOException {
        return Files.createFile(dir.resolve(name));
    }
}
