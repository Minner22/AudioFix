package pl.audiofix.ffmpeg;

import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Parsing of single lines printed by ffmpeg with -progress pipe:1, e.g.:
 * <pre>
 * out_time_us=12345678
 * speed=35.2x
 * progress=continue
 * </pre>
 */
class FfmpegRunnerProgressTest {

    private static final double DURATION = 60.0;   // seconds

    @Test
    void halfOfDurationIsHalfProgress() {
        assertEquals(OptionalDouble.of(0.5), FfmpegRunner.progressFrom("out_time_us=30000000", DURATION));
    }

    @Test
    void startIsZero() {
        assertEquals(OptionalDouble.of(0.0), FfmpegRunner.progressFrom("out_time_us=0", DURATION));
    }

    @Test
    void progressEndIsComplete() {
        assertEquals(OptionalDouble.of(1.0), FfmpegRunner.progressFrom("progress=end", DURATION));
    }

    @Test
    void progressContinueIsIgnored() {
        assertEquals(OptionalDouble.empty(), FfmpegRunner.progressFrom("progress=continue", DURATION));
    }

    @Test
    void otherKeysAreIgnored() {
        assertEquals(OptionalDouble.empty(), FfmpegRunner.progressFrom("speed=35.2x", DURATION));
        assertEquals(OptionalDouble.empty(), FfmpegRunner.progressFrom("total_size=1048576", DURATION));
        assertEquals(OptionalDouble.empty(), FfmpegRunner.progressFrom("out_time=00:00:30.000000", DURATION));
    }

    @Test
    void notAvailableTimeIsIgnored() {
        // ffmpeg prints N/A before the first packet is written
        assertEquals(OptionalDouble.empty(), FfmpegRunner.progressFrom("out_time_us=N/A", DURATION));
    }

    @Test
    void timeBeyondDurationIsCappedAtOne() {
        assertEquals(OptionalDouble.of(1.0), FfmpegRunner.progressFrom("out_time_us=61000000", DURATION));
    }

    @Test
    void negativeTimeIsCappedAtZero() {
        // can happen at the very start with some containers
        assertEquals(OptionalDouble.of(0.0), FfmpegRunner.progressFrom("out_time_us=-23220", DURATION));
    }

    @Test
    void unknownDurationGivesNoFractionButEndStillCompletes() {
        assertEquals(OptionalDouble.empty(), FfmpegRunner.progressFrom("out_time_us=30000000", 0));
        assertEquals(OptionalDouble.of(1.0), FfmpegRunner.progressFrom("progress=end", 0));
    }

    @Test
    void emptyLineIsIgnored() {
        assertEquals(OptionalDouble.empty(), FfmpegRunner.progressFrom("", DURATION));
    }
}
