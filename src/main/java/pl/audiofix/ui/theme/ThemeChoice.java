package pl.audiofix.ui.theme;

import javafx.application.ColorScheme;

/** What the user picked in the "Motyw" menu; SYSTEM follows the Windows app mode. */
public enum ThemeChoice {

    SYSTEM,
    LIGHT,
    DARK;

    /** The theme to show for this choice while Windows uses the given color scheme. */
    public Theme resolve(ColorScheme system) {

        return switch (this) {
            case LIGHT -> Theme.LIGHT;
            case DARK -> Theme.DARK;
            case SYSTEM -> system == ColorScheme.DARK ? Theme.DARK : Theme.LIGHT;
        };
    }

    /** Choice saved in the preferences; anything unknown (old version, edited registry) means SYSTEM. */
    public static ThemeChoice parse(String saved) {

        for (ThemeChoice choice : values()) {
            if (choice.name().equals(saved)) {
                return choice;
            }
        }

        return SYSTEM;
    }
}
