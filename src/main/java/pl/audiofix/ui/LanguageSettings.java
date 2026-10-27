package pl.audiofix.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Locale;
import java.util.prefs.Preferences;


public final class LanguageSettings {

    static final String KEY = "language";

    private final ObjectProperty<Language> language;

    public LanguageSettings() {

        this(Preferences.userNodeForPackage(LanguageSettings.class), Locale.getDefault());
    }

    public LanguageSettings(Preferences preferences, Locale system) {

        language = new SimpleObjectProperty<>(Language.parse(preferences.get(KEY, null)).orElse(Language.forSystem(system)));
        language.addListener((observable, old, picked) -> preferences.put(KEY, picked.name()));
    }

    public ObjectProperty<Language> languageProperty() {

        return language;
    }

    public Language getLanguage() {

        return language.get();
    }

    public void setLanguage(Language picked) {

        language.set(picked);
    }
}
