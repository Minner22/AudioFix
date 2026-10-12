package pl.audiofix.ui;

import pl.audiofix.model.AudioCodec;
import pl.audiofix.model.ConversionJob;
import pl.audiofix.model.ConversionMode;
import pl.audiofix.model.JobStatus;
import pl.audiofix.model.TrackPlan;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Texts next to the progress bar: what the running file does, its percent and time left, how it ended. */
final class ProgressTexts {

    private ProgressTexts() {
    }

    /** "Konwertowanie: ścieżki #1 i #3 (PCM 24-bit)", by the plan of the running file. */
    static String running(ConversionJob job) {

        if (job.getMode() == ConversionMode.QUICK) {
            return "Szybka konwersja (" + AudioCodec.PCM_S24LE.getLabel() + ")";
        }

        Map<AudioCodec, List<Integer>> tracksByCodec = new LinkedHashMap<>();
        for (TrackPlan plan : job.getPlans()) {
            if (plan.isConverted()) {
                tracksByCodec.computeIfAbsent(plan.getTargetCodec(), codec -> new ArrayList<>()).add(plan.getStream().index());
            }
        }

        if (tracksByCodec.isEmpty()) {
            return "Kopiowanie ścieżek";
        }

        return "Konwertowanie: " + tracksByCodec.entrySet().stream()
                .map(entry -> tracks(entry.getValue()) + " (" + entry.getKey().getLabel() + ")")
                .collect(Collectors.joining(", "));
    }

    /** "ścieżka #1", "ścieżki #1 i #3", "ścieżki #1, #2 i #3". */
    static String tracks(List<Integer> indexes) {

        List<String> numbers = indexes.stream().map(index -> "#" + index).toList();
        if (numbers.size() == 1) {
            return "ścieżka " + numbers.getFirst();
        }

        return "ścieżki " + String.join(", ", numbers.subList(0, numbers.size() - 1)) + " i " + numbers.getLast();
    }

    /** "Gotowe", "Anulowano" or "Błąd" once the file is no longer converted. */
    static String finished(JobStatus status) {

        return switch (status) {
            case DONE -> "Gotowe";
            case CANCELLED -> "Anulowano";
            case FAILED -> "Błąd";
            default -> "";
        };
    }

    /** "42%", or "42% (Pozostało ok. 02:15)" once the time left is known. */
    static String percent(double progress, Optional<Duration> remaining) {

        // rounded down, so 99.9% is not shown as done; the small margin keeps 0.29 * 100 = 28.999… at 29
        String percent = (int) Math.floor(progress * 100 + 1e-9) + "%";
        return remaining
                .map(left -> percent + " (Pozostało ok. " + TextFormats.duration(left) + ")")
                .orElse(percent);
    }
}
