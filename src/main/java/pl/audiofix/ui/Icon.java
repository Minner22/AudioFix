package pl.audiofix.ui;

import javafx.scene.Group;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;

import java.util.Locale;

/**
 * Small icon for a button, used in FXML as {@code <Icon glyph="ADD"/>}. The color comes from CSS
 * ({@code -fx-fill} of {@code .icon}), so it follows the theme and the button variant.
 * Paths: Heroicons v1, solid 20x20 (MIT license).
 */
public final class Icon extends Group {

    /** Drawn size in pixels; the paths are 20x20. */
    static final double SIZE = 14;

    public enum Glyph {
        ADD("M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z"),
        SETTINGS("M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106"
                + ".54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734"
                + " 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947"
                + "c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532"
                + " 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6"
                + " 3 3 0 000 6z"),
        THEME("M17.293 13.293A8 8 0 016.707 2.707a8.001 8.001 0 1010.586 10.586z"),
        ABOUT("M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1"
                + "a1 1 0 100-2v-3a1 1 0 00-1-1H9z"),
        REMOVE("M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382"
                + "l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8"
                + "a1 1 0 00-1-1z"),
        START("M10 18a8 8 0 100-16 8 8 0 000 16zM9.555 7.168A1 1 0 008 8v4a1 1 0 001.555.832l3-2a1 1 0 000-1.664"
                + "l-3-2z"),
        QUICK("M11.3 1.046A1 1 0 0112 2v5h4a1 1 0 01.82 1.573l-7 10A1 1 0 018 18v-5H4a1 1 0 01-.82-1.573l7-10"
                + "a1 1 0 011.12-.381z");

        private final String path;

        Glyph(String path) {
            this.path = path;
        }

        /** Style class of this glyph, e.g. "icon-add", for its own color. */
        public String styleClass() {
            return "icon-" + name().toLowerCase(Locale.ROOT);
        }
    }

    private final SVGPath shape = new SVGPath();
    private Glyph glyph;

    public Icon() {

        // even-odd: Heroicons cut holes (cog, trash lines, play triangle) with inner paths
        shape.setFillRule(FillRule.EVEN_ODD);
        shape.setScaleX(SIZE / 20);
        shape.setScaleY(SIZE / 20);
        shape.getStyleClass().add("icon");
        getChildren().add(shape);   // Group bounds include the scale, so the button lays out a 14px icon
    }

    public Icon(Glyph glyph) {
        this();
        setGlyph(glyph);
    }

    public Glyph getGlyph() {
        return glyph;
    }

    public void setGlyph(Glyph glyph) {

        if (this.glyph != null) {
            shape.getStyleClass().remove(this.glyph.styleClass());
        }
        this.glyph = glyph;
        shape.setContent(glyph.path);
        shape.getStyleClass().add(glyph.styleClass());
    }
}