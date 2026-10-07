package pl.audiofix.ui;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.audiofix.ffmpeg.*;
import pl.audiofix.model.AudioCodec;
import pl.audiofix.model.MediaInfo;
import pl.audiofix.model.StreamInfo;
import pl.audiofix.model.TrackPlan;
import pl.audiofix.model.TrackPlans;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static pl.audiofix.AudioFixApp.WINDOW_TITLE;

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
    private MediaInfo mediaInfo;
    private List<TrackPlan> plans = List.of();
    private File lastDirectory;
    private Path output;
    private ConversionTask conversion;

    public void setFfmpegPaths(FfmpegPaths ffmpegPaths) {

        this.ffmpegPaths = ffmpegPaths;
    }

    public void shutdown() {

        if (conversion != null && conversion.isRunning()) {
            conversion.stopNow();
        }
    }

    void showMedia(MediaInfo info) {

        mediaInfo = info;

        List<TrackPlan> loaded = info.streams().stream()
                .map(TrackPlan::defaultsFor)
                .toList();
        TrackPlans.normalizeDefaults(loaded);
        loaded.forEach(plan -> plan.keepProperty().addListener(observable -> TrackPlans.normalizeDefaults(loaded)));

        plans = loaded;

        setOutput(OutputPathResolver.defaultOutput(info.path()));
        changeOutputButton.setDisable(false);

        trackTable.getItems().setAll(plans.stream()
                .filter(plan -> TrackTableConfigurer.isShown(plan.getStream().type()))
                .toList());
        logArea.appendText("Wczytano " + info.path().getFileName() + ": " + info.streams().size() + " ścieżek" + System.lineSeparator());
    }

    @FXML
    private void initialize() {

        trackTable.setPlaceholder(new Label("Dodaj plik, aby zobaczyć jego ścieżki"));
        cancelButton.setDisable(true);

        TrackTableConfigurer.configureTable(trackTable);
        TrackTableConfigurer.configureKeep(keepColumn);
        TrackTableConfigurer.configureReadOnly(indexColumn, StreamInfo::index);
        TrackTableConfigurer.configureReadOnly(typeColumn, s -> TrackTableConfigurer.typeLabel(s.type()));
        TrackTableConfigurer.configureReadOnly(codecColumn, StreamInfo::codecLabel);
        TrackTableConfigurer.configureReadOnly(channelsColumn, s -> s.isAudio() ? s.channels() : null);
        TrackTableConfigurer.configureReadOnly(languageColumn, StreamInfo::language);
        TrackTableConfigurer.configureReadOnly(titleColumn, StreamInfo::title);
        TrackTableConfigurer.configureAction(actionColumn);
        TrackTableConfigurer.configureDefault(defaultColumn);

        outputField.setEditable(false);
        changeOutputButton.setDisable(true);
    }

    @FXML
    private void onAddFiles() {

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Wybierz film");
        chooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Filmy (*.mkv, *.mp4, *.m2ts)", "*.mkv", "*.mp4", "*.m2ts"));

        if (lastDirectory != null && lastDirectory.isDirectory()) {
            chooser.setInitialDirectory(lastDirectory);
        }

        File file = chooser.showOpenDialog(window());
        if (file == null) {
            return;
        }
        lastDirectory = file.getParentFile();
        loadFile(file.toPath());
    }

    void loadFile(Path file) {

        FfprobeService service = new FfprobeService(ffmpegPaths);
        Task<MediaInfo> task = new Task<>() {
            @Override
            protected MediaInfo call() throws Exception {
                return service.probe(file);
            }
        };

        task.setOnSucceeded(event -> {
            addFilesButton.setDisable(false);
            showMedia(task.getValue());
        });
        task.setOnFailed(event -> {
            addFilesButton.setDisable(false);
            showError("Nie można odczytać pliku", task.getException());
        });

        addFilesButton.setDisable(true);

        Thread.ofVirtual().start(task);
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

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Zapisz jako");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Matroska (*.mkv)", "*.mkv"));

        File initialDir = output.getParent().toFile();
        if (initialDir.isDirectory()) {
            chooser.setInitialDirectory(initialDir);
        }
        chooser.setInitialFileName(output.getFileName().toString());

        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }

        Path chosen = OutputPathResolver.withMkvExtension(file.toPath());
        if (OutputPathResolver.isSameFile(chosen, mediaInfo.path())) {
            showWarning("Nieprawidłowy plik wyjściowy",
                    "Plik wyjściowy nie może być tym samym plikiem co film źródłowy");
            return;
        }

        setOutput(chosen);
    }

    @FXML
    private void onStart() {

        List<String> errors = plans.isEmpty()
                ? List.of("Najpierw dodaj plik.")
                : TrackPlans.validate(plans);

        if (!errors.isEmpty()) {
            showWarning("Nie można rozpocząć konwersji", String.join(System.lineSeparator(), errors));
            return;
        }

        startConversion();
    }

    @FXML
    private void onCancel() {

        if (conversion != null) {
            conversion.cancel();
        }
    }

    private void startConversion() {

        List<String> command = new CommandBuilder().build(ffmpegPaths.ffmpeg(), mediaInfo.path(), output, plans);
        appendLog(CommandBuilder.toCommandLine(command));

        ConversionTask task = new ConversionTask(command, output, mediaInfo.durationSec(), this::appendLog);
        task.setOnSucceeded(event -> {
            setRunning(false);
            appendLog("Gotowe: " + task.output());
            showInfo("Konwersja zakończona", task.output().toString());
        });
        task.setOnFailed(event -> {
            setRunning(false);
            resetProgress();
            showError("Konwersja nie powiodła się", task.getException());
        });
        task.setOnCancelled(event -> {
            setRunning(false);
            resetProgress();
            appendLog("Anulowano");
        });

        conversion = task;
        progressBar.progressProperty().bind(task.progressProperty());
        setRunning(true);
        Thread.ofVirtual().start(task);
    }

    private void resetProgress() {

        progressBar.progressProperty().unbind();
        progressBar.setProgress(0);
    }

    private void setRunning(boolean running) {

        startButton.setDisable(running);
        cancelButton.setDisable(!running);
        addFilesButton.setDisable(running);
        ffmpegSettingsButton.setDisable(running);
        changeOutputButton.setDisable(running);
        trackTable.setDisable(running);
    }

    private void appendLog(String line) {

        logArea.appendText(line + System.lineSeparator());
    }

    private void showError(String header, Throwable error) {

        log.warn(header, error);
        showAlert(Alert.AlertType.ERROR, header, error.getMessage());
    }

    private void showWarning(String header, String content) {

        log.warn("{}: {}", header, content);
        showAlert(Alert.AlertType.WARNING, header, content);
    }

    private void showInfo(String header, String content) {

        log.info("{}: {}", header, content);
        showAlert(Alert.AlertType.INFORMATION, header, content);
    }

    private void showAlert(Alert.AlertType type, String header, String content) {

        Alert alert = new Alert(type);
        alert.initOwner(window());
        alert.setTitle(WINDOW_TITLE);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.show();
    }

    private void setOutput(Path path) {

        output = path;
        outputField.setText(path.toString());
    }

    private Window window() {

        return addFilesButton.getScene().getWindow();
    }
}
