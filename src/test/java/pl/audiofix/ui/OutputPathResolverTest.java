package pl.audiofix.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
}
