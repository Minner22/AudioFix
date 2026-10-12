package pl.audiofix.ui;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * Time left of a conversion from its progress and the time since it started:
 * left = elapsed × (1 − progress) / progress, averaged over the last few readings.
 */
final class EtaEstimator {

    /** Before this the estimate jumps around too much to be shown. */
    static final Duration MIN_ELAPSED = Duration.ofSeconds(5);
    static final double MIN_PROGRESS = 0.01;
    static final int SAMPLES = 5;

    private final LongSupplier nanoTime;
    private final long start;
    private final Deque<Double> recent = new ArrayDeque<>();

    /** Starts counting now; nanoTime is System::nanoTime, tests pass their own clock. */
    EtaEstimator(LongSupplier nanoTime) {

        this.nanoTime = nanoTime;
        this.start = nanoTime.getAsLong();
    }

    /** Time left at this progress (0..1), or empty while it is too early or the progress is unknown. */
    Optional<Duration> remaining(double progress) {

        if (!(progress > 0)) {
            return Optional.empty();   // also NaN and the negative "indeterminate" value
        }
        if (progress >= 1) {
            return Optional.of(Duration.ZERO);
        }

        long elapsed = nanoTime.getAsLong() - start;
        if (elapsed < MIN_ELAPSED.toNanos() || progress < MIN_PROGRESS) {
            return Optional.empty();
        }

        recent.addLast(elapsed * (1 - progress) / progress);
        if (recent.size() > SAMPLES) {
            recent.removeFirst();
        }
        double average = recent.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        return Optional.of(Duration.ofNanos(Math.round(average)));
    }
}
