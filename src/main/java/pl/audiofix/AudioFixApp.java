package pl.audiofix;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.audiofix.ffmpeg.FfmpegLocator;
import pl.audiofix.ffmpeg.FfmpegPaths;
import pl.audiofix.ui.FfmpegSetup;
import pl.audiofix.ui.MainController;
import pl.audiofix.ui.theme.Theme;
import pl.audiofix.ui.theme.ThemeFonts;
import pl.audiofix.ui.theme.ThemeManager;

import java.util.Optional;

public class AudioFixApp extends Application {

    public static final String WINDOW_TITLE = "AudioFix";

    static final double MIN_WIDTH = 960;
    static final double MIN_HEIGHT = 600;

    private static final Logger log = LoggerFactory.getLogger(AudioFixApp.class);

    private MainController controller;

    public static void main(String[] args) {

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        log.info("AudioFix {} starting (Java {})", AppVersion.current(), Runtime.version());

        ThemeFonts.load();
        ThemeManager themes = new ThemeManager();
        Theme.currentProperty().bind(themes.themeProperty());   // before the first alert, ffmpeg setup may show one

        Optional<FfmpegPaths> ffmpegPaths = FfmpegSetup.resolve(new FfmpegLocator());
        if (ffmpegPaths.isEmpty()) {
            Platform.exit();
            return;
        }

        FXMLLoader loader = new FXMLLoader(AudioFixApp.class.getResource("ui/main-view.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        controller.setFfmpegPaths(ffmpegPaths.get());
        controller.setHostServices(getHostServices());
        controller.setThemeManager(themes);

        primaryStage.setScene(createScene(root));
        configureWindow(primaryStage);
        primaryStage.show();
    }

    static String windowTitle() {

        return WINDOW_TITLE + " " + AppVersion.current();
    }

    static Scene createScene(Parent root) {

        Scene scene = new Scene(root, 1100, 700);
        Theme.apply(scene);

        return  scene;
    }

    @Override
    public void stop() throws Exception {

        if (controller != null) {
            controller.shutdown();
        }
    }

    static void configureWindow(Stage stage) {

        stage.setTitle(windowTitle());
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
    }
}
