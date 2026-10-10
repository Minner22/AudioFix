package pl.audiofix.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.audiofix.model.MediaInfo;
import pl.audiofix.model.StreamInfo;
import pl.audiofix.model.StreamType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.audiofix.ui.FxTestSupport.onFxThread;

/**
 * Output file row of the main window: default name after loading a file, read-only field, [Zmień…] state.
 */
class OutputFieldTest {

    @TempDir
    Path dir;

    private MainController controller;
    private TextField outputField;
    private Button changeOutputButton;

    @BeforeAll
    static void startJavaFx() {
        FxTestSupport.startJavaFx();
    }

    @BeforeEach
    void loadView() throws Exception {
        onFxThread(() -> {
            FXMLLoader loader = FxTestSupport.loadMainView();
            Parent root = loader.getRoot();
            controller = loader.getController();
            outputField = (TextField) root.lookup("#outputField");
            changeOutputButton = (Button) root.lookup("#changeOutputButton");
        });
    }

    @Test
    void beforeLoadingFileOutputIsEmptyAndCannotBeChanged() {
        assertTrue(outputField.getText() == null || outputField.getText().isEmpty());
        assertTrue(changeOutputButton.isDisable());
    }

    @Test
    void outputFieldIsReadOnly() {
        // the output is chosen with [Zmień…] - typing could produce an invalid path
        assertFalse(outputField.isEditable());
    }

    @Test
    void loadingFileSetsDefaultOutputNextToIt() throws Exception {
        Path input = dir.resolve("Film.mkv");

        onFxThread(() -> controller.addMedia(media(input)));

        assertEquals(dir.resolve("Film_fixed.mkv").toString(), outputField.getText());
        assertFalse(changeOutputButton.isDisable());
    }

    @Test
    void existingOutputIsNotOverwrittenByDefault() throws Exception {
        Files.createFile(dir.resolve("Film_fixed.mkv"));

        onFxThread(() -> controller.addMedia(media(dir.resolve("Film.mkv"))));

        assertEquals(dir.resolve("Film_fixed (1).mkv").toString(), outputField.getText());
    }

    @Test
    void loadingAnotherFileReplacesOutput() throws Exception {
        onFxThread(() -> controller.addMedia(media(dir.resolve("Film.mkv"))));
        onFxThread(() -> controller.addMedia(media(dir.resolve("Serial S01E01.mp4"))));

        assertEquals(dir.resolve("Serial S01E01_fixed.mkv").toString(), outputField.getText());
    }

    private static MediaInfo media(Path path) {
        return new MediaInfo(path, 10, List.of(
                new StreamInfo(0, StreamType.VIDEO, "h264", null, 0, null, "und", null, true),
                new StreamInfo(1, StreamType.AUDIO, "dts", null, 6, null, "eng", null, true)));
    }
}
