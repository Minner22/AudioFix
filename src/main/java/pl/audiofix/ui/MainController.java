package pl.audiofix.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.audiofix.ffmpeg.FfmpegLocator;
import pl.audiofix.ffmpeg.FfmpegPaths;
import pl.audiofix.model.AudioCodec;
import pl.audiofix.model.TrackPlan;

public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @FXML private Button addFilesButton;
    @FXML private Button ffmpegSettingsButton;

    @FXML private ListView<String> queueList;
    @FXML private Button removeFromQueueButton;

    @FXML private TableView<TrackPlan> trackTable;
    @FXML private TableColumn<TrackPlan, Boolean> keepColumn;
    @FXML private TableColumn<TrackPlan, Integer> indexColumn;
    @FXML private TableColumn<TrackPlan, String> typeColumn;
    @FXML private TableColumn<TrackPlan, String> codecColumn;
    @FXML private TableColumn<TrackPlan, Integer> channelsColumn;
    @FXML private TableColumn<TrackPlan, String> languageColumn;
    @FXML private TableColumn<TrackPlan, String> titleColumn;
    @FXML private TableColumn<TrackPlan, AudioCodec> actionColumn;
    @FXML private TableColumn<TrackPlan, Boolean> defaultColumn;

    @FXML private TextField outputField;
    @FXML private Button changeOutputButton;

    @FXML private Button startButton;
    @FXML private Button cancelButton;
    @FXML private ProgressBar progressBar;
    @FXML private TextArea logArea;

    private FfmpegPaths ffmpegPaths;

    public void setFfmpegPaths(FfmpegPaths ffmpegPaths) {

        this.ffmpegPaths = ffmpegPaths;
    }

    @FXML
    private void initialize() {

        trackTable.setPlaceholder(new Label("Dodaj plik, aby zobaczyć jego ścieżki"));
        cancelButton.setDisable(true);
    }

    @FXML
    private void onAddFiles() {

        log.info("Add files clicked");
    }

    @FXML
    private void onFfmpegSettings() {

        FfmpegSetup.choose(new FfmpegLocator(), window())
                .ifPresent(paths -> {
                    ffmpegPaths = paths;
                    log.info("ffmpeg changed to {}", paths.ffmpeg());
                });
    }

    @FXML
    private void onRemoveFromQueue() {

        log.info("Remove from queue clicked");
    }

    @FXML
    private void onChangeOutput() {

        log.info("Change output clicked");
    }

    @FXML
    private void onStart() {

        log.info("Start clicked");
    }

    @FXML
    private void onCancel() {

        log.info("Cancel clicked");
    }

    private Window window() {

        return addFilesButton.getScene().getWindow();
    }
}
