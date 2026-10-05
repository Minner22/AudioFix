package pl.audiofix;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pl.audiofix.ffmpeg.FfmpegLocator;
import pl.audiofix.ffmpeg.FfmpegPaths;
import pl.audiofix.ui.FfmpegSetup;
import pl.audiofix.ui.MainController;

import java.util.Optional;

public class AudioFixApp extends Application {

    public static final String WINDOW_TITLE = "AudioFix";

    public static void main(String[] args) {

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        Optional<FfmpegPaths> ffmpegPaths = FfmpegSetup.resolve(new FfmpegLocator());
        if (ffmpegPaths.isEmpty()) {
            Platform.exit();
            return;
        }

        FXMLLoader loader = new FXMLLoader(AudioFixApp.class.getResource("ui/main-view.fxml"));
        Parent root = loader.load();

        MainController controller = loader.getController();
        controller.setFfmpegPaths(ffmpegPaths.get());

        primaryStage.setScene(new Scene(root, 1100, 700));
        primaryStage.setTitle(WINDOW_TITLE);
        primaryStage.show();
    }
}
