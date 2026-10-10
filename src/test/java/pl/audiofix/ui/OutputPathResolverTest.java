package pl.audiofix.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputPathResolverTest {

    @TempDir
    Path dir;

    // ---------------------------------------------------------------- defaultOutput

    @Test
    void defaultOutputIsNextToInputWithSuffix() {
        Path input = dir.resolve("Film.mkv");

        assertEquals(dir.resolve("Film_fixed.mkv"), OutputPathResolver.defaultOutput(input));
    }

    @Test
    void mp4AndM2tsInputGiveMkvOutput() {
        assertEquals(dir.resolve("Film_fixed.mkv"), OutputPathResolver.defaultOutput(dir.resolve("Film.mp4")));
        assertEquals(dir.resolve("Film_fixed.mkv"), OutputPathResolver.defaultOutput(dir.resolve("Film.m2ts")));
    }

    @Test
    void dotsInNameAreKept() {
        // only the real extension is removed - "REMUX-FraMeSToR" is part of the name
        Path input = dir.resolve("Drive.2011.UHD.BluRay.2160p.REMUX-FraMeSToR.mkv");

        assertEquals(dir.resolve("Drive.2011.UHD.BluRay.2160p.REMUX-FraMeSToR_fixed.mkv"),
                OutputPathResolver.defaultOutput(input));
    }

    @Test
    void uppercaseExtensionIsReplaced() {
        assertEquals(dir.resolve("FILM_fixed.mkv"), OutputPathResolver.defaultOutput(dir.resolve("FILM.MKV")));
    }

    @Test
    void nameWithSpacesAndPolishCharacters() {
        assertEquals(dir.resolve("Mój film ąę_fixed.mkv"), OutputPathResolver.defaultOutput(dir.resolve("Mój film ąę.mkv")));
    }

    @Test
    void inputWithoutExtension() {
        assertEquals(dir.resolve("Film_fixed.mkv"), OutputPathResolver.defaultOutput(dir.resolve("Film")));
    }

    @Test
    void existingOutputGetsNumber() throws IOException {
        Files.createFile(dir.resolve("Film_fixed.mkv"));

        assertEquals(dir.resolve("Film_fixed (1).mkv"), OutputPathResolver.defaultOutput(dir.resolve("Film.mkv")));
    }

    @Test
    void nextFreeNumberIsUsed() throws IOException {
        Files.createFile(dir.resolve("Film_fixed.mkv"));
        Files.createFile(dir.resolve("Film_fixed (1).mkv"));
        Files.createFile(dir.resolve("Film_fixed (2).mkv"));

        assertEquals(dir.resolve("Film_fixed (3).mkv"), OutputPathResolver.defaultOutput(dir.resolve("Film.mkv")));
    }

    @Test
    void defaultOutputIsNeverTheInput() throws IOException {
        // input already ends with _fixed.mkv - e.g. fixing a file a second time
        Path input = Files.createFile(dir.resolve("Film_fixed.mkv"));

        Path output = OutputPathResolver.defaultOutput(input);

        assertEquals(dir.resolve("Film_fixed_fixed.mkv"), output);
        assertFalse(OutputPathResolver.isSameFile(input, output));
    }

    @Test
    void relativeInputGivesAbsoluteOutput() {
        assertTrue(OutputPathResolver.defaultOutput(Path.of("Film.mkv")).isAbsolute());
    }

    // ---------------------------------------------------------------- withMkvExtension

    @Test
    void mkvExtensionIsAddedWhenMissing() {
        assertEquals(dir.resolve("wynik.mkv"), OutputPathResolver.withMkvExtension(dir.resolve("wynik")));
    }

    @Test
    void mkvExtensionIsNotDuplicated() {
        assertEquals(dir.resolve("wynik.mkv"), OutputPathResolver.withMkvExtension(dir.resolve("wynik.mkv")));
        assertEquals(dir.resolve("WYNIK.MKV"), OutputPathResolver.withMkvExtension(dir.resolve("WYNIK.MKV")));
    }

    @Test
    void dotsInChosenNameAreNotTreatedAsExtension() {
        // user typed a name with dots but without .mkv - nothing may be cut off
        assertEquals(dir.resolve("Drive.2011.REMUX-FraMeSToR.mkv"),
                OutputPathResolver.withMkvExtension(dir.resolve("Drive.2011.REMUX-FraMeSToR")));
    }

    @Test
    void otherVideoExtensionGetsMkvAppended() {
        // output is always Matroska (PCM does not fit in MP4) - the chosen name is kept as typed
        assertEquals(dir.resolve("wynik.mp4.mkv"), OutputPathResolver.withMkvExtension(dir.resolve("wynik.mp4")));
    }

    // ---------------------------------------------------------------- isSameFile

    @Test
    void samePathIsSameFile() {
        assertTrue(OutputPathResolver.isSameFile(dir.resolve("Film.mkv"), dir.resolve("Film.mkv")));
    }

    @Test
    void differentCaseIsSameFileOnWindows() {
        assertTrue(OutputPathResolver.isSameFile(dir.resolve("Film.mkv"), dir.resolve("FILM.MKV")));
    }

    @Test
    void notNormalizedPathIsSameFile() {
        assertTrue(OutputPathResolver.isSameFile(dir.resolve("Film.mkv"), dir.resolve("sub").resolve("..").resolve("Film.mkv")));
    }

    @Test
    void differentFilesAreNotSame() {
        assertFalse(OutputPathResolver.isSameFile(dir.resolve("Film.mkv"), dir.resolve("Film_fixed.mkv")));
    }

    // ---------------------------------------------------------------- outputs taken by the queue (#13)

    @Test
    void outputTakenByAnotherQueuedFileIsSkipped() {
        Path taken = dir.resolve("Film_fixed.mkv");   // not on disk yet - another job will write it

        assertEquals(dir.resolve("Film_fixed (1).mkv"), OutputPathResolver.defaultOutput(dir.resolve("Film.mp4"), List.of(taken)));
    }

    @Test
    void takenAndExistingNamesAreBothSkipped() throws IOException {
        Files.createFile(dir.resolve("Film_fixed.mkv"));
        Path taken = dir.resolve("Film_fixed (1).mkv");

        assertEquals(dir.resolve("Film_fixed (2).mkv"), OutputPathResolver.defaultOutput(dir.resolve("Film.mkv"), List.of(taken)));
    }

    @Test
    void takenNameIsComparedIgnoringCase() {
        Path taken = dir.resolve("FILM_FIXED.MKV");

        assertEquals(dir.resolve("Film_fixed (1).mkv"), OutputPathResolver.defaultOutput(dir.resolve("Film.mkv"), List.of(taken)));
    }

    @Test
    void unrelatedTakenNamesChangeNothing() {
        assertEquals(dir.resolve("Film_fixed.mkv"),
                OutputPathResolver.defaultOutput(dir.resolve("Film.mkv"), List.of(dir.resolve("Inny_fixed.mkv"))));
    }

    // ---------------------------------------------------------------- inFolder (#59)

    @Test
    void inFolderKeepsFileName() throws IOException {
        Path target = Files.createDirectory(dir.resolve("Seriale"));

        assertEquals(target.resolve("Odcinek 1_fixed.mkv"),
                OutputPathResolver.inFolder(dir.resolve("Odcinek 1_fixed.mkv"), target, List.of()));
    }

    @Test
    void inFolderKeepsCustomName() throws IOException {
        Path target = Files.createDirectory(dir.resolve("Seriale"));

        assertEquals(target.resolve("Mój wynik.mkv"), OutputPathResolver.inFolder(dir.resolve("Mój wynik.mkv"), target, List.of()));
    }

    @Test
    void inFolderSkipsExistingFile() throws IOException {
        Path target = Files.createDirectory(dir.resolve("Seriale"));
        Files.createFile(target.resolve("Film_fixed.mkv"));

        assertEquals(target.resolve("Film_fixed (1).mkv"), OutputPathResolver.inFolder(dir.resolve("Film_fixed.mkv"), target, List.of()));
    }

    @Test
    void inFolderSkipsNameTakenInQueue() throws IOException {
        Path target = Files.createDirectory(dir.resolve("Seriale"));
        List<Path> taken = List.of(target.resolve("Film_fixed.mkv"), target.resolve("Film_fixed (1).mkv"));

        assertEquals(target.resolve("Film_fixed (2).mkv"), OutputPathResolver.inFolder(dir.resolve("Film_fixed.mkv"), target, taken));
    }
}
