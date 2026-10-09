package pl.audiofix.ui.theme;

import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class ThemeFonts {

    static final List<String> FILES = List.of("Geist-Regular.ttf", "Geist-Bold.ttf", "JetBrainsMono-Regular.ttf");

    private static final Logger log = LoggerFactory.getLogger(ThemeFonts.class);

    private ThemeFonts() {}

    public static List<Font> load() {

        List<Font> loaded = new ArrayList<>();
        for (String file : FILES) {
            try (InputStream in = ThemeFonts.class.getResourceAsStream("fonts/" + file)) {
                Font font = in == null ? null : Font.loadFont(in, 12);
                if (font == null) {
                    log.warn("Cannot load font {}", file);
                } else {
                    loaded.add(font);
                }
            } catch (IOException e) {
                log.warn("Cannot load font {}", file, e);
            }
        }

        return loaded;
    }
}
