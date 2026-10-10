package pl.audiofix.ui;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.audiofix.ffmpeg.*;
import pl.audiofix.model.AudioCodec;
import pl.audiofix.model.ConversionJob;
import pl.audiofix.model.ConversionMode;
import pl.audiofix.model.JobStatus;
import pl.audiofix.model.MediaInfo;
import pl.audiofix.model.StreamInfo;
import pl.audiofix.model.TrackPlan;
import pl.audiofix.model.TrackPlans;
import pl.audiofix.queue.JobQueue;
import pl.audiofix.ui.theme.Theme;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static pl.audiofix.AudioFixApp.WINDOW_TITLE;

public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @FXML private Button addFilesButton;
    @FXML private Button ffmpegSettingsButton;

    @FXML private ListView<ConversionJob> queueList;
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
    @FXML private Button aboutButton;
    @FXML private Button quickConvertButton;


    private final ListChangeListener<String> logFollower = change -> {
        while (change.next()) {
            change.getAddedSubList().forEach(this::appendLog);
        }
    };

    private final InvalidationListener shownStatusListener = observable -> updateControls();

    private FfmpegPaths ffmpegPaths;
    private File lastDirectory;
    private HostServices hostServices;

    private final JobQueue queue = JobQueue.withFfmpeg(() -> ffmpegPaths.ffmpeg());

    private ConversionJob shown;
    private List<ConversionJob> batch = List.of();
    private boolean loading;

    public void setFfmpegPaths(FfmpegPaths ffmpegPaths) {

        this.ffmpegPaths = ffmpegPaths;
    }

    public void setHostServices(HostServices hostServices) {

        this.hostServices = hostServices;
    }

    public void shutdown() {

        queue.shutdown();
    }

    @FXML
    private void initialize() {

        queueList.setItems(queue.getJobs());
        queueList.setCellFactory(list -> new QueueCell());
        queueList.setPlaceholder(new Label("Dodaj pliki, aby utworzyć kolejkę"));
        queueList.getSelectionModel().selectedItemProperty().addListener((observable, previous, job) -> showJob(job));

        queue.getJobs().addListener((ListChangeListener<ConversionJob>) change -> updateControls());
        queue.runningProperty().addListener((observable, wasRunning, running) -> {
            updateControls();
            if (!running) {
                onQueueFinished();
            }
        });
        queue.currentJobProperty().addListener((observable, previous, job) -> {
            if (job != null) {
                progressBar.progressProperty().bind(job.progressProperty());   // stays on the last job when idle
            }
        });

        trackTable.setPlaceholder(new Label("Dodaj plik, aby zobaczyć jego ścieżki"));
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
        updateControls();
    }

    @FXML
    private void onAddFiles() {

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Wybierz filmy");
        chooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Filmy (*.mkv, *.mp4, *.m2ts)", "*.mkv", "*.mp4", "*.m2ts"));

        if (lastDirectory != null && lastDirectory.isDirectory()) {
            chooser.setInitialDirectory(lastDirectory);
        }

        List<File> files = chooser.showOpenMultipleDialog(window());
        if (files == null || files.isEmpty()) {
            return;
        }
        lastDirectory = files.getFirst().getParentFile();
        addFiles(files.stream().map(File::toPath).toList());
    }

    void addFiles(List<Path> files) {

        List<Path> alreadyQueued = files.stream().filter(this::isQueued).toList();
        List<Path> toAdd = files.stream().filter(file -> !isQueued(file)).toList();
        if (!alreadyQueued.isEmpty()) {
            showWarning("Pominięto pliki, które już są w kolejce", fileNames(alreadyQueued));
        }
        if (toAdd.isEmpty()) {
            return;
        }

        FfprobeService service = new FfprobeService(ffmpegPaths);
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() {
                List<String> failures = new ArrayList<>();
                for (Path file : toAdd) {
                    try {
                        MediaInfo info = service.probe(file);
                        Platform.runLater(() -> addMedia(info));
                    } catch (RuntimeException e) {
                        log.warn("Cannot read {}", file, e);
                        failures.add(file.getFileName() + ": " + e.getMessage());
                    }
                }

                return failures;
            }
        };

        task.setOnSucceeded(event -> {
            setLoading(false);
            if (!task.getValue().isEmpty()) {
                showWarning("Nie można odczytać części plików", String.join(System.lineSeparator(), task.getValue()));
            }
        });
        task.setOnFailed(event -> {
            setLoading(false);
            showError("Nie można odczytać plików", task.getException());
        });

        setLoading(true);
        Thread.ofVirtual().start(task);
    }

    ConversionJob addMedia(MediaInfo info) {

        List<TrackPlan> plans = info.streams().stream()
                .map(TrackPlan::defaultsFor)
                .toList();
        TrackPlans.normalizeDefaults(plans);
        plans.forEach(plan -> plan.keepProperty().addListener(observable -> TrackPlans.normalizeDefaults(plans)));

        Path output = OutputPathResolver.defaultOutput(info.path(), outputsInUse(null));
        ConversionJob job = new ConversionJob(info, plans, output, ConversionMode.PLANNED);
        job.appendLog("Wczytano " + info.path().getFileName() + ": " + info.streams().size() + " ścieżek");

        queue.add(job);
        queueList.getSelectionModel().select(job);

        return job;
    }

    private void showJob(ConversionJob job) {

        if (shown != null) {
            shown.getLog().removeListener(logFollower);
            shown.statusProperty().removeListener(shownStatusListener);
            outputField.textProperty().unbind();
        }
        shown = job;

        if (job == null) {
            trackTable.getItems().clear();
            outputField.clear();
            logArea.clear();
        } else {
            trackTable.getItems().setAll(job.getPlans().stream()
                    .filter(plan -> TrackTableConfigurer.isShown(plan.getStream().type()))
                    .toList());
            outputField.textProperty().bind(Bindings.createStringBinding(
                    () -> job.getOutput().toString(), job.outputProperty()));
            logArea.clear();
            job.getLog().forEach(this::appendLog);
            job.getLog().addListener(logFollower);
            job.statusProperty().addListener(shownStatusListener);
        }
        updateControls();
    }

    @FXML
    private void onChangeOutput() {

        ConversionJob job = shown;
        if (job == null || !job.isEditable()) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Zapisz jako");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Matroska (*.mkv)", "*.mkv"));

        File initialDir = job.getOutput().getParent().toFile();
        if (initialDir.isDirectory()) {
            chooser.setInitialDirectory(initialDir);
        }
        chooser.setInitialFileName(job.getOutput().getFileName().toString());

        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }
        changeOutput(job, file.toPath());
    }

    void changeOutput(ConversionJob job, Path chosenFile) {

        Path chosen = OutputPathResolver.withMkvExtension(chosenFile);
        if (OutputPathResolver.isSameFile(chosen, job.getInput())) {
            showWarning("Nieprawidłowy plik wyjściowy",
                    "Plik wyjściowy nie może być tym samym plikiem co film źródłowy");
            return;
        }
        if (outputsInUse(job).stream().anyMatch(path -> OutputPathResolver.isSameFile(path, chosen))) {
            showWarning("Nieprawidłowy plik wyjściowy",
                    "Do tego pliku zapisze już inny film z kolejki");
            return;
        }
        job.setOutput(chosen);
    }

    @FXML
    private void onRemoveFromQueue() {

        ConversionJob job = shown;
        if (job == null) {
            return;
        }
        int index = queue.getJobs().indexOf(job);
        if (!queue.remove(job)) {
            return;
        }
        if (queue.getJobs().isEmpty()) {
            queueList.getSelectionModel().clearSelection();
        } else {
            queueList.getSelectionModel().select(Math.min(index, queue.getJobs().size() - 1));
        }
    }

    @FXML
    private void onStart() {

        List<ConversionJob> pending = pendingJobs();
        if (pending.isEmpty()) {
            showWarning("Nie można rozpocząć konwersji", "Brak plików oczekujących na konwersję. Dodaj pliki.");
            return;
        }

        List<String> errors = new ArrayList<>();
        ConversionJob firstInvalid = null;
        for (ConversionJob job : pending) {
            if (job.getMode() == ConversionMode.QUICK) {
                continue;
            }
            List<String> jobErrors = TrackPlans.validate(job.getPlans());
            if (!jobErrors.isEmpty() && firstInvalid == null) {
                firstInvalid = job;
            }
            jobErrors.forEach(error -> errors.add(job.getInput().getFileName() + ": " + error));
        }
        if (firstInvalid != null) {
            queueList.getSelectionModel().select(firstInvalid);
            showWarning("Nie można rozpocząć konwersji", String.join(System.lineSeparator(), errors));
            return;
        }

        startQueue(pending);
    }

    @FXML
    private void onQuickConvert() {

        List<ConversionJob> pending = pendingJobs();
        if (pending.isEmpty()) {
            showWarning("Nie można rozpocząć konwersji", "Brak plików oczekujących na konwersję. Dodaj pliki.");
            return;
        }

        pending.forEach(job -> job.setMode(ConversionMode.QUICK));
        startQueue(pending);
    }

    @FXML
    private void onCancel() {

        queue.cancelCurrent();
    }

    private void startQueue(List<ConversionJob> pending) {

        batch = List.copyOf(pending);
        queue.start();
    }

    private void onQueueFinished() {

        List<ConversionJob> finished = batch;
        batch = List.of();

        long done = count(finished, JobStatus.DONE);
        long cancelled = count(finished, JobStatus.CANCELLED);
        List<ConversionJob> failed = finished.stream().filter(job -> job.getStatus() == JobStatus.FAILED).toList();
        if (done == 0 && failed.isEmpty()) {
            return;   // nothing converted, e.g. the only file was cancelled
        }

        List<String> lines = new ArrayList<>();
        lines.add("Gotowe: " + done);
        if (!failed.isEmpty()) {
            lines.add("Błędy: " + failed.size());
        }
        if (cancelled > 0) {
            lines.add("Anulowane: " + cancelled);
        }
        for (ConversionJob job : failed) {
            lines.add("• " + job.getInput().getFileName() + ": " + firstLine(job.getErrorMessage()));
        }
        String summary = String.join(System.lineSeparator(), lines);

        if (failed.isEmpty()) {
            showInfo("Konwersja zakończona", summary);
        } else {
            showWarning("Konwersja zakończona z błędami", summary + System.lineSeparator() + "Szczegóły w logu pliku.");
        }
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
    private void onAbout() {

        try {
            AboutDialog.show(window(), ffmpegPaths, hostServices);
        } catch (RuntimeException e) {
            showError("Nie można otworzyć okna „O programie”", e);
        }
    }

    private void updateControls() {

        boolean running = queue.isRunning();
        boolean editable = shown != null && shown.isEditable();

        addFilesButton.setDisable(running || loading);
        ffmpegSettingsButton.setDisable(running);
        startButton.setDisable(running);
        quickConvertButton.setDisable(running);
        cancelButton.setDisable(!running);
        removeFromQueueButton.setDisable(shown == null || shown.getStatus() == JobStatus.RUNNING);
        trackTable.setDisable(shown != null && !editable);
        changeOutputButton.setDisable(!editable);
    }

    private void setLoading(boolean value) {

        loading = value;
        updateControls();
    }

    private List<ConversionJob> pendingJobs() {

        return queue.getJobs().stream().filter(job -> job.getStatus() == JobStatus.PENDING).toList();
    }

    private boolean isQueued(Path file) {

        return queue.getJobs().stream()
                .filter(job -> !job.getStatus().isFinished())
                .anyMatch(job -> OutputPathResolver.isSameFile(job.getInput(), file));
    }

    private List<Path> outputsInUse(ConversionJob except) {

        return queue.getJobs().stream()
                .filter(job -> job != except && !job.getStatus().isFinished())
                .map(ConversionJob::getOutput)
                .toList();
    }

    private static long count(List<ConversionJob> jobs, JobStatus status) {

        return jobs.stream().filter(job -> job.getStatus() == status).count();
    }

    private static String firstLine(String text) {

        return text == null ? "" : text.lines().findFirst().orElse("");
    }

    private static String fileNames(List<Path> files) {

        return String.join(System.lineSeparator(), files.stream().map(file -> file.getFileName().toString()).toList());
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
        Theme.apply(alert);
        alert.setTitle(WINDOW_TITLE);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.show();
    }

    private Window window() {

        return addFilesButton.getScene().getWindow();
    }
}
