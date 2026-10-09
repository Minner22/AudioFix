package pl.audiofix.ui;

import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import pl.audiofix.ffmpeg.FfmpegLocator;
import pl.audiofix.ffmpeg.FfmpegPaths;
import pl.audiofix.ui.theme.Theme;

import java.io.File;
import java.util.Optional;

import static pl.audiofix.AudioFixApp.WINDOW_TITLE;

public final class FfmpegSetup {

    private FfmpegSetup() {

    }

    public static Optional<FfmpegPaths> resolve(FfmpegLocator locator) {

        Optional<FfmpegPaths> found = locator.locate();

        if (found.isPresent()) {

            return found;
        }

        Alert info = new Alert(Alert.AlertType.INFORMATION);
        Theme.apply(info);
        info.setTitle(WINDOW_TITLE);
        info.setHeaderText("Nie znaleziono ffmpeg");
        info.setContentText("Wskaż plik ffmpeg.exe. W tym samym folderze musi znajdować się plik ffprobe.exe.");
        info.showAndWait();

        return choose(locator, null);
    }

    public static Optional<FfmpegPaths> choose(FfmpegLocator locator, Window owner) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Wskaż plik ffmpeg.exe");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Program (*.exe)", "*.exe"));
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        while (true) {

            File file = fileChooser.showOpenDialog(owner);

            if (file == null) {

                return Optional.empty();
            }

            try {
                return Optional.of(locator.save(file.toPath()));
            } catch (IllegalArgumentException e) {
                Alert error = new Alert(Alert.AlertType.ERROR);
                Theme.apply(error);
                error.initOwner(owner);
                error.setTitle(WINDOW_TITLE);
                error.setHeaderText("Nieprawidłowy plik ffmpeg");
                error.setContentText(e.getMessage());
                error.showAndWait();
            }
        }

    }
}