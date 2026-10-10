package pl.audiofix.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


public final class TrackPlans {

    private TrackPlans() {
    }

    public static void setDefault(List<TrackPlan> plans, TrackPlan chosen) {

        StreamType type = chosen.getStream().type();
        if (type != StreamType.AUDIO && type != StreamType.SUBTITLE) {
            return;
        }

        chosen.setKeep(true);
        for (TrackPlan plan : plans) {
            if (plan.getStream().type() == type) {
                plan.setMakeDefault(plan == chosen);
            }
        }
    }

    public static void clearSubtitleDefault(List<TrackPlan> plans) {

        for (TrackPlan plan : plans) {
            if (plan.getStream().type() == StreamType.SUBTITLE) {
                plan.setMakeDefault(false);
            }
        }
    }

    public static void normalizeDefaults(List<TrackPlan> plans) {

        normalize(plans, StreamType.AUDIO, true);
        normalize(plans, StreamType.SUBTITLE, false);
    }

    public static List<String> validate(List<TrackPlan> plans) {

        List<String> errors = new ArrayList<>();

        List<TrackPlan> keptAudio = kept(plans, StreamType.AUDIO);
        if (keptAudio.isEmpty()) {
            errors.add("Zostaw co najmniej jedną ścieżkę audio.");
        } else if (countDefaults(keptAudio) != 1) {
            errors.add("Wybierz dokładnie jedną domyślną ścieżkę audio.");
        }

        if (countDefaults(kept(plans, StreamType.SUBTITLE)) > 1) {
            errors.add("Domyślne mogą być najwyżej jedne napisy.");
        }

        return errors;
    }

    private static void normalize(List<TrackPlan> plans, StreamType type, boolean required) {

        TrackPlan current = null;
        TrackPlan firstKept = null;

        for (TrackPlan plan : plans) {
            if (plan.getStream().type() != type) {
                continue;
            }
            if (!plan.isKeep()) {
                plan.setMakeDefault(false);
                continue;
            }

            if (firstKept == null) {
                firstKept = plan;
            }

            if (plan.isMakeDefault()) {
                if (current == null) {
                    current = plan;
                } else {
                    plan.setMakeDefault(false);
                }
            }
        }

        if (current == null && required && firstKept != null) {
            firstKept.setMakeDefault(true);
        }
    }

    private static List<TrackPlan> kept(List<TrackPlan> plans, StreamType type) {

        return plans.stream()
                .filter(TrackPlan::isKeep)
                .filter(plan -> plan.getStream().type() == type)
                .toList();
    }

    private static long countDefaults(List<TrackPlan> plans) {

        return plans.stream()
                .filter(TrackPlan::isMakeDefault)
                .count();
    }

    public static Optional<String> layoutDifference(List<TrackPlan> reference, List<TrackPlan> other) {

        if (reference.size() != other.size()) {

            return Optional.of("inna liczba ścieżek: " + other.size() + " zamiast " + reference.size());
        }

        for (int i = 0; i < reference.size(); i++) {
            StreamInfo expected = reference.get(i).getStream();
            StreamInfo actual = other.get(i).getStream();
            String track = "ścieżka #" + actual.index() + ": ";

            if (expected.type() != actual.type()) {
                return Optional.of(track + "inny typ ścieżki");
            }
            if (!Objects.equals(expected.codec(), actual.codec())) {
                return Optional.of(track + "kodek " + actual.codec() + " zamiast " + expected.codec());
            }
            if (expected.channels() != actual.channels()) {
                return Optional.of(track + "kanały " + actual.channels() + " zamiast " + expected.channels());
            }
            if (!Objects.equals(expected.language(), actual.language())) {
                return Optional.of(track + "język " + actual.language() + " zamiast " + expected.language());
            }
        }

        return Optional.empty();
    }

    public static void copySettings(List<TrackPlan> from, List<TrackPlan> to) {

        layoutDifference(from, to).ifPresent(difference -> {
            throw new IllegalArgumentException("Different track layout: " + difference);
        });

        // keep first: changing it re-normalizes defaults through listeners, so defaults are set last
        for (int i = 0; i < from.size(); i++) {
            to.get(i).setKeep(from.get(i).isKeep());
        }
        for (int i = 0; i < from.size(); i++) {
            to.get(i).setTargetCodec(from.get(i).getTargetCodec());
            to.get(i).setMakeDefault(from.get(i).isMakeDefault());
        }
    }
}
