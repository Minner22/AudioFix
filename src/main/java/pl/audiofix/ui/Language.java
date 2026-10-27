package pl.audiofix.ui;

import java.util.Locale;
import java.util.Optional;


public enum Language {

    POLISH(Locale.of("pl")),
    ENGLISH(Locale.ENGLISH);

    private final Locale locale;

    Language(Locale locale) {

        this.locale = locale;
    }

    public Locale locale() {

        return locale;
    }

    static Language forSystem(Locale system) {

        return POLISH.locale.getLanguage().equals(system.getLanguage()) ? POLISH : ENGLISH;
    }

    static Optional<Language> parse(String saved) {

        for (Language language : values()) {
            if (language.name().equals(saved)) {
                return Optional.of(language);
            }
        }

        return Optional.empty();
    }
}
