package pl.audiofix.model;

public enum PlanProblem {

    NO_AUDIO,
    DEFAULT_AUDIO_COUNT,
    MULTIPLE_DEFAULT_SUBTITLES;

    public String messageKey() {

        return "plan.problem." + name();
    }
}
