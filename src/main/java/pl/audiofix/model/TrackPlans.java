package pl.audiofix.model;

import java.util.ArrayList;
import java.util.List;


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
}
