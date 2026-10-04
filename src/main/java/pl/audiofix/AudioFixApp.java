package pl.audiofix;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import pl.audiofix.ffmpeg.FfmpegLocator;
import pl.audiofix.ffmpeg.FfmpegPaths;

import java.io.File;
import java.util.Optional;

public class AudioFixApp extends Application {

    private static final String WINDOW_TITLE = "AudioFix";

    public static void main(String[] args) {

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        Optional<FfmpegPaths> ffmpegPaths = resolveFfmpeg();
        if (ffmpegPaths.isEmpty()) {
            Platform.exit();
            return;
        }

        FXMLLoader loader = new FXMLLoader(AudioFixApp.class.getResource("ui/main-view.fxml"));
        Parent root = loader.load();

        primaryStage.setScene(new Scene(root, 1100, 700));
        primaryStage.setTitle(WINDOW_TITLE);
        primaryStage.show();
    }

    private Optional<FfmpegPaths> resolveFfmpeg() {

        FfmpegLocator locator = new FfmpegLocator();

        Optional<FfmpegPaths> found = locator.locate();

        if (found.isPresent()) {

            return found;
        }

        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle(WINDOW_TITLE);
        info.setHeaderText("Nie znaleziono ffmpeg");
        info.setContentText("Wskaż plik ffmpeg.exe. W tym samym folderze musi znajdować się plik ffprobe.exe.");
        info.showAndWait();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Wskaż plik ffmpeg.exe");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Program (*.exe)", "*.exe"));
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        while (true) {

            File file = fileChooser.showOpenDialog(null);

            if (file == null) {

                return Optional.empty();
            }

            try {
                return Optional.of(locator.save(file.toPath()));
            } catch (IllegalArgumentException e) {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle(WINDOW_TITLE);
                error.setHeaderText("Nieprawidłowy plik ffmpeg");
                error.setContentText(e.getMessage());
                error.showAndWait();
            }
        }
    }

}
