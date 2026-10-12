package pl.audiofix.ui;

import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static pl.audiofix.ui.FxTestSupport.callOnFxThread;

/** The window icon: every size loads and has the size its name says. */
class AppIconTest {

    @BeforeAll
    static void startJavaFx() {
        FxTestSupport.startJavaFx();
    }

    @Test
    void everySizeLoads() throws Exception {
        List<Image> images = callOnFxThread(AppIcon::images);

        assertEquals(AppIcon.SIZES.size(), images.size());
        for (int i = 0; i < images.size(); i++) {
            Image image = images.get(i);
            int size = AppIcon.SIZES.get(i);
            assertFalse(image.isError(), "icon " + size + " did not load");
            assertEquals(size, image.getWidth(), "icon " + size);
            assertEquals(size, image.getHeight(), "icon " + size);
        }
    }
}
