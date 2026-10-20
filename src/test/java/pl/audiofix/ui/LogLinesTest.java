package pl.audiofix.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Kinds of log lines, from what JobQueue, ConversionJob and ffmpeg really write. */
class LogLinesTest {

    // ---------------------------------------------------------------- command

    @ParameterizedTest
    @ValueSource(strings = {
            "C:\\ffmpeg\\bin\\ffmpeg.exe -hide_banner -y -i in.mkv out.mkv",
            "\"C:\\Program Files\\ffmpeg\\bin\\ffmpeg.exe\" -hide_banner -y -i in.mkv out.mkv",
            "/usr/bin/ffmpeg -hide_banner -i in.mkv out.mkv",
            "ffmpeg -i in.mkv out.mkv",
    })
    void commandStartsWithFfmpegExecutable(String line) {
        assertEquals(LogLines.Kind.COMMAND, LogLines.kind(line));
    }

    @Test
    void ffmpegInTheMiddleIsNoCommand() {
        assertEquals(LogLines.Kind.PLAIN, LogLines.kind("Wczytano ffmpeg test.mkv: 3 ścieżek"));
    }

    @Test
    void commandWithErrorInFileNameIsStillCommand() {
        // a film called "Error.mkv" must not turn the command red
        assertEquals(LogLines.Kind.COMMAND, LogLines.kind("ffmpeg -i \"D:\\Filmy\\Error.mkv\" out.mkv"));
    }

    // ---------------------------------------------------------------- errors and warnings

    @ParameterizedTest
    @ValueSource(strings = {
            "Error opening input file D:\\x.mkv.",
            "Invalid data found when processing input",
            "Conversion failed!",
            "[aac @ 0x1] error while decoding",
            "Błąd: ffmpeg exited with code 1",
    })
    void errorsAreRecognized(String line) {
        assertEquals(LogLines.Kind.ERROR, LogLines.kind(line));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "[matroska @ 0x1] Warning: something",
            "Codec AVOption b is deprecated",
            "Anulowano",
    })
    void warningsAreRecognized(String line) {
        assertEquals(LogLines.Kind.WARNING, LogLines.kind(line));
    }

    @Test
    void resultLinesAreRecognizedInEnglish() {
        // MainController writes them in the current language (#30)
        Messages.use(java.util.Locale.ENGLISH);
        try {
            assertEquals(LogLines.Kind.SUCCESS, LogLines.kind("Done: Drive_fixed.mkv"));
            assertEquals(LogLines.Kind.ERROR, LogLines.kind("Error: ffmpeg exited with an error (code 1):"));
            assertEquals(LogLines.Kind.WARNING, LogLines.kind("Cancelled"));
        } finally {
            Messages.use(java.util.Locale.getDefault());
        }
    }

    @Test
    void doneIsSuccess() {
        assertEquals(LogLines.Kind.SUCCESS, LogLines.kind("Gotowe: Drive_fixed.mkv"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Stream mapping:",
            "  Stream #0:1 -> #0:1 (truehd (native) -> pcm_s24le (native))",
            "Wczytano Drive.mkv: 9 ścieżek",
            "",
    })
    void otherLinesArePlain(String line) {
        assertEquals(LogLines.Kind.PLAIN, LogLines.kind(line));
    }

    @Test
    void versionLineIsVersion() {
        assertEquals(LogLines.Kind.VERSION, LogLines.kind("AudioFix 0.4.0 | ffmpeg 7.0.1"));
    }

    @Test
    void styleClassNamesKind() {
        assertEquals("log-command", LogLines.Kind.COMMAND.styleClass());
        assertEquals("log-success", LogLines.Kind.SUCCESS.styleClass());
    }

    // ---------------------------------------------------------------- version line

    @Test
    void versionLineHasAppAndFfmpegVersion() {
        String ffmpeg = "ffmpeg version 7.0.1-essentials_build-www.gyan.dev Copyright (c) 2000-2024 the FFmpeg developers";

        assertEquals("AudioFix 0.4.0 | ffmpeg 7.0.1-essentials_build-www.gyan.dev", LogLines.versionLine("0.4.0", ffmpeg));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void unknownFfmpegVersionLeavesOnlyApp(String ffmpeg) {
        assertEquals("AudioFix 0.4.0", LogLines.versionLine("0.4.0", ffmpeg));
    }

    @Test
    void unusualFfmpegLineIsKeptWhole() {
        assertEquals("AudioFix 0.4.0 | ffmpeg N-112233-gabc", LogLines.versionLine("0.4.0", "N-112233-gabc"));
    }
}
