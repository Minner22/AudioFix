package pl.audiofix.ui;

import javafx.application.HostServices;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.audiofix.AppVersion;
import pl.audiofix.ffmpeg.FfmpegPaths;
import pl.audiofix.ffmpeg.FfmpegVersion;
import pl.audiofix.ui.theme.Theme;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import static pl.audiofix.AudioFixApp.WINDOW_TITLE;

final class AboutDialog {

    private static final Logger log = LoggerFactory.getLogger(AboutDialog.class);

    private AboutDialog() {

    }
    
    static Stage show(Window owner, FfmpegPaths ffmpegPaths, HostServices hostServices) {

        FXMLLoader loader = new FXMLLoader(AboutDialog.class.getResource("about-view.fxml"));
        Parent root;

        try {
            root = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Path ffmpeg = ffmpegPaths.ffmpeg();
        AboutController controller = loader.getController();
        controller.setHostServices(hostServices);
        AboutInfo info = AboutInfo.of(AppVersion.current(), ffmpeg, null);
        controller.show(info);

        Task<String> version = createVersion(ffmpeg, controller, info);
        Thread.ofVirtual().start(version);

        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("O programie - " + WINDOW_TITLE);

        Scene scene = new Scene(root);
        Theme.apply(scene);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        return stage;
    }

    private static Task<String> createVersion(Path ffmpeg, AboutController controller, AboutInfo info) {
        Task<String> version = new Task<>() {
            @Override
            protected String call() {
                return FfmpegVersion.firstLine(ffmpeg).orElse(AboutInfo.UNKNOWN);
            }
        };

        version.setOnSucceeded(event -> controller.show(info.withFfmpegVersion(version.getValue())));
        version.setOnFailed(event -> {
            log.warn("Cannot read ffmpeg version", version.getException());
            controller.show(info.withFfmpegVersion(AboutInfo.UNKNOWN));
        });
        return version;
    }
}
