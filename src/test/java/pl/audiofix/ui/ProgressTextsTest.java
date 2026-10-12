package pl.audiofix.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pl.audiofix.model.AudioCodec;
import pl.audiofix.model.ConversionJob;
import pl.audiofix.model.ConversionMode;
import pl.audiofix.model.JobStatus;
import pl.audiofix.model.MediaInfo;
import pl.audiofix.model.StreamInfo;
import pl.audiofix.model.StreamType;
import pl.audiofix.model.TrackPlan;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** What the bottom bar says while a file is converted and after. */
class ProgressTextsTest {

    // ---------------------------------------------------------------- running

    @Test
    void namesConvertedTracksAndFormat() {
        ConversionJob drive = drive(ConversionMode.PLANNED);   // TrueHD and DTS -> PCM 24-bit

        assertEquals("Konwertowanie: ścieżki #1 i #3 (PCM 24-bit)", ProgressTexts.running(drive));
    }

    @Test
    void oneConvertedTrack() {
        ConversionJob drive = drive(ConversionMode.PLANNED);
        plan(drive, 3).setTargetCodec(AudioCodec.COPY);

        assertEquals("Konwertowanie: ścieżka #1 (PCM 24-bit)", ProgressTexts.running(drive));
    }

    @Test
    void tracksGroupedByFormat() {
        ConversionJob drive = drive(ConversionMode.PLANNED);
        plan(drive, 2).setTargetCodec(AudioCodec.EAC3);

        assertEquals("Konwertowanie: ścieżki #1 i #3 (PCM 24-bit), ścieżka #2 (E-AC3 (Dolby Digital+))",
                ProgressTexts.running(drive));
    }

    @Test
    void removedTrackIsNotNamed() {
        ConversionJob drive = drive(ConversionMode.PLANNED);
        plan(drive, 3).setKeep(false);

        assertEquals("Konwertowanie: ścieżka #1 (PCM 24-bit)", ProgressTexts.running(drive));
    }

    @Test
    void nothingConvertedIsCopying() {
        ConversionJob drive = drive(ConversionMode.PLANNED);
        plan(drive, 1).setTargetCodec(AudioCodec.COPY);
        plan(drive, 3).setTargetCodec(AudioCodec.COPY);

        assertEquals("Kopiowanie ścieżek", ProgressTexts.running(drive));
    }

    @Test
    void quickConversionIgnoresPlan() {
        assertEquals("Szybka konwersja (PCM 24-bit)", ProgressTexts.running(drive(ConversionMode.QUICK)));
    }

    @Test
    void trackListInPolish() {
        assertEquals("ścieżka #4", ProgressTexts.tracks(List.of(4)));
        assertEquals("ścieżki #1 i #3", ProgressTexts.tracks(List.of(1, 3)));
        assertEquals("ścieżki #1, #2 i #3", ProgressTexts.tracks(List.of(1, 2, 3)));
    }

    // ---------------------------------------------------------------- finished

    @Test
    void resultOfFinishedFile() {
        assertEquals("Gotowe", ProgressTexts.finished(JobStatus.DONE));
        assertEquals("Anulowano", ProgressTexts.finished(JobStatus.CANCELLED));
        assertEquals("Błąd", ProgressTexts.finished(JobStatus.FAILED));
        assertEquals("", ProgressTexts.finished(JobStatus.PENDING));
    }

    // ---------------------------------------------------------------- percent

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "0.0,   0%",
            "0.425, 42%",
            "0.29,  29%",
            "0.999, 99%",
            "1.0,   100%",
    })
    void percentIsRoundedDown(double progress, String expected) {
        // 99.9% must not show 100% while ffmpeg is still writing
        assertEquals(expected, ProgressTexts.percent(progress, Optional.empty()));
    }

    @Test
    void percentWithTimeLeft() {
        assertEquals("42% (Pozostało ok. 02:15)", ProgressTexts.percent(0.42, Optional.of(Duration.ofSeconds(135))));
    }

    // ---------------------------------------------------------------- helpers

    /** Video, TrueHD, AC3, DTS: TrueHD and DTS are converted by default. */
    static ConversionJob drive(ConversionMode mode) {
        MediaInfo media = new MediaInfo(Path.of("D:\\Filmy\\Drive.mkv"), 6000, List.of(
                new StreamInfo(0, StreamType.VIDEO, "hevc", null, 0, null, "und", null, true),
                new StreamInfo(1, StreamType.AUDIO, "truehd", null, 8, "7.1", "eng", null, true),
                new StreamInfo(2, StreamType.AUDIO, "ac3", null, 6, "5.1", "eng", null, false),
                new StreamInfo(3, StreamType.AUDIO, "dts", "DTS-HD MA", 6, "5.1", "eng", null, false)));
        List<TrackPlan> plans = media.streams().stream().map(TrackPlan::defaultsFor).toList();
        return new ConversionJob(media, plans, Path.of("D:\\Filmy\\Drive_fixed.mkv"), mode);
    }

    private static TrackPlan plan(ConversionJob job, int streamIndex) {
        return job.getPlans().stream().filter(plan -> plan.getStream().index() == streamIndex).findFirst().orElseThrow();
    }
}
