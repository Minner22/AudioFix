package pl.audiofix.ui;

import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.List;

public final class AppIcon {

    static final List<Integer> SIZES = List.of(16, 32, 48, 256);

    private AppIcon() {}

    public static void apply(Stage stage) {

        stage.getIcons().setAll(images());
    }

    static List<Image> images() {

        return SIZES.stream()
                .map(size -> new Image(AppIcon.class.getResource("icons/audiofix-" + size + ".png").toExternalForm()))
                .toList();
    }
}
