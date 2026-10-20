package pl.audiofix.ui;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;


public final class Messages {

    static final String BUNDLE = "pl.audiofix.i18n.messages";

    private static volatile Locale locale = Locale.getDefault();
    private static volatile ResourceBundle bundle = load(locale);

    private Messages() {
    }

    public static ResourceBundle bundle() {

        return bundle;
    }

    public static Locale locale() {

        return locale;
    }

    public static void use(Locale language) {

        bundle = load(language);
        locale = language;
    }

    public static String get(String key, Object... args) {

        return format(bundle, locale, key, args);
    }

    public static String count(String key, long n) {

        return get(key + "." + TextFormats.pluralForm(n), n);
    }

    static String format(ResourceBundle from, Locale language, String key, Object... args) {

        String pattern = from.getString(key);
        return args.length == 0 ? pattern : new MessageFormat(pattern, language).format(args);
    }

    static ResourceBundle load(Locale language) {

        return ResourceBundle.getBundle(BUNDLE, language,
                ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
    }
}
