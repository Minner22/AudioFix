package pl.audiofix.ui;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Time left with a fake clock, so nothing waits for real. */
class EtaEstimatorTest {

    private final AtomicLong now = new AtomicLong(1_000_000_000L);   // any start value, only differences count
    private final EtaEstimator eta = new EtaEstimator(now::get);

    @Test
    void halfDoneAfterOneMinuteLeavesOneMinute() {
        after(60);

        assertEquals(Optional.of(Duration.ofSeconds(60)), eta.remaining(0.5));
    }

    @Test
    void nothingIsShownInTheFirstSeconds() {
        after(4);

        assertEquals(Optional.empty(), eta.remaining(0.5), "too early, the estimate jumps around");
    }

    @Test
    void nothingIsShownBelowOnePercent() {
        after(30);

        assertEquals(Optional.empty(), eta.remaining(0.005));
    }

    @Test
    void finishedLeavesNothing() {
        after(1);   // even before the threshold

        assertEquals(Optional.of(Duration.ZERO), eta.remaining(1.0));
    }

    @Test
    void unknownProgressGivesNoEstimate() {
        after(60);

        assertEquals(Optional.empty(), eta.remaining(0));
        assertEquals(Optional.empty(), eta.remaining(-1), "ProgressBar.INDETERMINATE_PROGRESS");
        assertEquals(Optional.empty(), eta.remaining(Double.NaN));
    }

    @Test
    void oneSlowReadingIsSmoothedOut() {
        // steady 1% per second: 10 s at 10%, 11 s at 11%… each reading says 90, 89, 88 s left
        for (int second = 10; second <= 13; second++) {
            now.set(start() + Duration.ofSeconds(second).toNanos());
            eta.remaining(second / 100.0);
        }
        // a stall: 20 s at 14% would say about 123 s on its own
        now.set(start() + Duration.ofSeconds(20).toNanos());
        Duration left = eta.remaining(0.14).orElseThrow();

        assertTrue(left.toSeconds() < 100, "spike should be averaged with the steady readings: " + left);
        assertTrue(left.toSeconds() > 87, "but still move up a little: " + left);
    }

    @Test
    void oldReadingsStopCounting() {
        // a stall at the start, then a long steady run: after SAMPLES readings only the steady ones count
        after(10);
        eta.remaining(0.01);   // says 990 s left
        for (int i = 1; i <= EtaEstimator.SAMPLES; i++) {
            now.set(start() + Duration.ofSeconds(100).toNanos());
            eta.remaining(0.5);   // says 100 s left
        }

        assertEquals(Optional.of(Duration.ofSeconds(100)), eta.remaining(0.5));
    }

    // ---------------------------------------------------------------- helpers

    private static long start() {
        return 1_000_000_000L;
    }

    private void after(int seconds) {
        now.set(start() + Duration.ofSeconds(seconds).toNanos());
    }
}
