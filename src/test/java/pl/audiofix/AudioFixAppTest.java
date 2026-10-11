package pl.audiofix;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import pl.audiofix.ui.theme.Theme;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AudioFixAppTest {

    @Test
    public void mainViewFxmlIsOnClasspath() {

        assertNotNull(AudioFixApp.class.getResource("ui/main-view.fxml"));
    }

    @Test
    public void mainSceneUsesCurrentTheme() {
        CompletableFuture<Void> started = new CompletableFuture<>();
        try {
            Platform.startup(() -> started.complete(null));
        } catch (IllegalStateException alreadyStarted) {
            started.complete(null);
        }
        Platform.setImplicitExit(false);
        started.orTimeout(10, TimeUnit.SECONDS).join();

        CompletableFuture<Scene> scene = new CompletableFuture<>();
        Platform.runLater(() -> scene.complete(AudioFixApp.createScene(new VBox())));
        Scene created = scene.orTimeout(10, TimeUnit.SECONDS).join();

        assertEquals(Theme.current().stylesheets(), created.getStylesheets());
        assertEquals(1100, created.getWidth());
        assertEquals(700, created.getHeight());
    }

    @Test
    public void windowHasMinimumSizeAndVersionInTitle() {
        mainSceneUsesCurrentTheme();   // starts JavaFX

        CompletableFuture<Stage> stage = new CompletableFuture<>();
        Platform.runLater(() -> {
            Stage window = new Stage();
            AudioFixApp.configureWindow(window);
            stage.complete(window);
        });
        Stage configured = stage.orTimeout(10, TimeUnit.SECONDS).join();

        assertEquals(960, configured.getMinWidth());
        assertEquals(600, configured.getMinHeight());
        assertEquals(AudioFixApp.windowTitle(), configured.getTitle());
    }
}
