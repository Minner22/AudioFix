package pl.audiofix.ui;

import javafx.geometry.Bounds;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.audiofix.ui.FxTestSupport.callOnFxThread;

class IconTest {

    @BeforeAll
    static void startJavaFx() {
        FxTestSupport.startJavaFx();
    }

    @ParameterizedTest
    @EnumSource(Icon.Glyph.class)
    void everyGlyphFitsInIconSize(Icon.Glyph glyph) throws Exception {
        Bounds bounds = callOnFxThread(() -> new Icon(glyph).getLayoutBounds());

        assertTrue(bounds.getWidth() > Icon.SIZE / 2, glyph + " too small: " + bounds);
        assertTrue(bounds.getWidth() <= Icon.SIZE + 0.5 && bounds.getHeight() <= Icon.SIZE + 0.5, glyph + ": " + bounds);
    }

    @Test
    void holesInIconsStayOpen() throws Exception {
        // with the default non-zero rule the cog, trash and play icons are drawn as solid blobs
        SVGPath shape = callOnFxThread(() -> shape(new Icon(Icon.Glyph.SETTINGS)));

        assertEquals(FillRule.EVEN_ODD, shape.getFillRule());
    }

    @Test
    void glyphHasStyleClassesForColor() throws Exception {
        SVGPath shape = callOnFxThread(() -> shape(new Icon(Icon.Glyph.ADD)));

        assertTrue(shape.getStyleClass().contains("icon"));
        assertTrue(shape.getStyleClass().contains("icon-add"));
    }

    @Test
    void changingGlyphReplacesShapeAndStyleClass() throws Exception {
        SVGPath shape = callOnFxThread(() -> {
            Icon icon = new Icon(Icon.Glyph.ADD);
            icon.setGlyph(Icon.Glyph.REMOVE);
            return shape(icon);
        });

        assertTrue(shape.getStyleClass().contains("icon-remove"));
        assertFalse(shape.getStyleClass().contains("icon-add"), "old glyph class must go");
        assertTrue(shape.getContent().startsWith("M9 2"), "trash path");
    }

    @Test
    void fxmlCanCreateIconWithoutArguments() throws Exception {
        // <Icon glyph="ADD"/> needs a no-argument constructor and a setter
        Icon icon = callOnFxThread(() -> {
            Icon created = new Icon();
            created.setGlyph(Icon.Glyph.ABOUT);
            return created;
        });

        assertEquals(Icon.Glyph.ABOUT, icon.getGlyph());
    }

    private static SVGPath shape(Icon icon) {
        return (SVGPath) icon.lookup(".icon");
    }
}
