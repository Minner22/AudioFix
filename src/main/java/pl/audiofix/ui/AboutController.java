package pl.audiofix.ui;

import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class AboutController {

    static final String REPOSITORY_URL = "https://github.com/Minner22/AudioFix";

    @FXML private Label appVersionLabel;
    @FXML private Hyperlink repositoryLink;
    @FXML private Label ffmpegLabel;
    @FXML private Label environmentLabel;
    @FXML private TextArea licensesArea;
    @FXML private Button copyButton;
    @FXML private Button closeButton;

    private AboutInfo info;
    private HostServices hostServices;

    @FXML
    private void initialize() {

        repositoryLink.setText(REPOSITORY_URL);
        licensesArea.setText(licenses());
    }

    void setHostServices(HostServices hostServices) {

        this.hostServices = hostServices;
    }

    void show(AboutInfo info) {

        this.info = info;
        appVersionLabel.setText("AudioFix " + info.appVersion());

        String ffmpegLabelText = "ffmpeg: " +
                (info.ffmpegVersion() == null ? "sprawdzanie…" : info.ffmpegVersion()) +
                "\n" +
                Objects.toString(info.ffmpegPath(), AboutInfo.UNKNOWN);
        ffmpegLabel.setText(ffmpegLabelText);

        String environmentLabelText = "Java " + info.javaVersion() + ", " +
                "JavaFX " + info.javafxVersion() + ", " +
                info.os();
        environmentLabel.setText(environmentLabelText);
    }

    @FXML
    private void onRepository() {

        if (hostServices != null) {
            hostServices.showDocument(REPOSITORY_URL);
        }
    }

    @FXML
    private void onCopy() {

        ClipboardContent content = new ClipboardContent();
        content.putString(info.toClipboardText());
        Clipboard.getSystemClipboard().setContent(content);
    }

    @FXML
    private void onClose() {

        ((Stage) closeButton.getScene().getWindow()).close();
    }

    static String licenses() {

        try (InputStream inputStream = AboutController.class.getResourceAsStream("licenses.txt")) {

            if (inputStream == null) {
                return "";
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException _) {
            return "";
        }
    }
}
