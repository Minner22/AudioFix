package pl.audiofix.ui;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.audiofix.AppVersion;
import pl.audiofix.ffmpeg.*;
import pl.audiofix.model.AudioCodec;
import pl.audiofix.model.ConversionJob;
import pl.audiofix.model.ConversionMode;
import pl.audiofix.model.JobStatus;
import pl.audiofix.model.MediaInfo;
import pl.audiofix.model.PlanProblem;
import pl.audiofix.model.StreamInfo;
import pl.audiofix.model.StreamType;
import pl.audiofix.model.TrackPlan;
import pl.audiofix.model.TrackPlans;
import pl.audiofix.model.UserMessage;
import pl.audiofix.queue.JobQueue;
import pl.audiofix.ui.theme.Theme;
import pl.audiofix.ui.theme.ThemeChoice;
import pl.audiofix.ui.theme.ThemeManager;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static pl.audiofix.AudioFixApp.WINDOW_TITLE;

public class MainController {

    private static final PseudoClass DRAG_OVER = PseudoClass.getPseudoClass("drag-over");

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @FXML private BorderPane rootPane;

    @FXML private Button addFilesButton;
    @FXML private Button ffmpegSettingsButton;
    @FXML private RadioMenuItem systemThemeItem;
    @FXML private RadioMenuItem lightThemeItem;
    @FXML private RadioMenuItem darkThemeItem;
    @FXML private ToggleGroup languageGroup;
    @FXML private ToggleButton polishButton;
    @FXML private ToggleButton englishButton;

    @FXML private Label queueTitleLabel;
    @FXML private Label queueRemainingLabel;
    @FXML private ListView<ConversionJob> queueList;
    @FXML private Button removeFromQueueButton;

    @FXML private Label tracksTitleLabel;
    @FXML private Label tracksFileLabel;
    @FXML private Label tracksInfoLabel;
    @FXML private Label trackCountLabel;
    @FXML private TableView<TrackPlan> trackTable;

    @FXML private TableColumn<TrackPlan, Boolean> keepColumn;
    @FXML private TableColumn<TrackPlan, Integer> indexColumn;
    @FXML private TableColumn<TrackPlan, StreamType> typeColumn;
    @FXML private TableColumn<TrackPlan, StreamInfo> codecColumn;
    @FXML private TableColumn<TrackPlan, String> channelsColumn;
    @FXML private TableColumn<TrackPlan, String> languageColumn;
    @FXML private TableColumn<TrackPlan, String> titleColumn;
    @FXML private TableColumn<TrackPlan, AudioCodec> actionColumn;
    @FXML private TableColumn<TrackPlan, Boolean> defaultColumn;

    @FXML private TextField outputField;
    @FXML private Button changeOutputButton;
    @FXML private Button applyToAllButton;
    @FXML private Button outputFolderButton;

    @FXML private Button startButton;
    @FXML private Button cancelButton;
    @FXML private Label progressStatusLabel;
    @FXML private Label progressPercentLabel;
    @FXML private ProgressBar progressBar;
    @FXML private ListView<String> logList;
    @FXML private Button aboutButton;
    @FXML private Button quickConvertButton;


    private final InvalidationListener shownStatusListener = observable -> updateControls();

    private FfmpegPaths ffmpegPaths;
    private String ffmpegVersionLine;
    private ProgressView progressView;
    private File lastDirectory;
    private HostServices hostServices;

    private final JobQueue queue = JobQueue.withFfmpeg(() -> ffmpegPaths.ffmpeg());

    private ConversionJob shown;
    private List<ConversionJob> batch = List.of();
    private boolean loading;

    public void setFfmpegPaths(FfmpegPaths ffmpegPaths) {

        this.ffmpegPaths = ffmpegPaths;

        // for the first line of every file's log; read once in the background, ffmpeg -version takes a moment
        Task<String> version = new Task<>() {
            @Override
            protected String call() {
                return FfmpegVersion.firstLine(ffmpegPaths.ffmpeg()).orElse(null);
            }
        };
        version.setOnSucceeded(event -> ffmpegVersionLine = version.getValue());
        Thread.ofVirtual().start(version);
    }

    public void setHostServices(HostServices hostServices) {

        this.hostServices = hostServices;
    }

    /** Ties the "Motyw" menu to the saved choice; before this call the menu items do nothing. */
    public void setThemeManager(ThemeManager themes) {

        Map<ThemeChoice, RadioMenuItem> items = Map.of(
                ThemeChoice.SYSTEM, systemThemeItem,
                ThemeChoice.LIGHT, lightThemeItem,
                ThemeChoice.DARK, darkThemeItem);
        items.get(themes.getChoice()).setSelected(true);
        items.forEach((choice, item) -> item.setOnAction(event -> themes.setChoice(choice)));
    }

    public void setLanguageSettings(LanguageSettings languages, Map<String, Object> view) {

        Map<Language, ToggleButton> buttons = Map.of(Language.POLISH, polishButton, Language.ENGLISH, englishButton);
        buttons.get(languages.getLanguage()).setSelected(true);

        languageGroup.selectedToggleProperty().addListener((observable, previous, selected) -> {
            if (selected == null) {
                previous.setSelected(true);
                return;
            }
            languages.setLanguage(selected == polishButton ? Language.POLISH : Language.ENGLISH);
        });
        languages.languageProperty().addListener((observable, old, language) -> switchLanguage(language, view));
    }

    void switchLanguage(Language language, Map<String, Object> view) {

        Messages.use(language.locale());
        FxmlTexts.translate(MainController.class.getResource("main-view.fxml"), view);
        refreshTexts();
    }

    private void refreshTexts() {

        setPlaceholders();
        LogConsole.translate(logList);
        updateQueueHeader();
        updateTracksHeader();
        queueList.refresh();
        trackTable.refresh();
        progressView.refresh();
    }

    private void setPlaceholders() {

        queueList.setPlaceholder(placeholder("queue.placeholder"));
        trackTable.setPlaceholder(placeholder("tracks.placeholder"));
    }

    private static Label placeholder(String key) {

        Label text = new Label(Messages.get(key));
        text.setWrapText(true);
        text.setTextAlignment(TextAlignment.CENTER);
        return text;
    }

    public void shutdown() {

        queue.shutdown();
    }

    @FXML
    private void initialize() {

        queueList.setItems(queue.getJobs());
        queueList.setCellFactory(list -> new QueueCell());
        setPlaceholders();
        queueList.getSelectionModel().selectedItemProperty().addListener((observable, previous, job) -> showJob(job));

        queue.getJobs().addListener((ListChangeListener<ConversionJob>) change -> {
            updateControls();
            updateQueueHeader();
        });
        queue.runningProperty().addListener((observable, wasRunning, running) -> {
            updateControls();
            if (!running) {
                onQueueFinished();
            }
        });
        progressView = new ProgressView(progressBar, progressStatusLabel, progressPercentLabel, System::nanoTime);
        queue.currentJobProperty().addListener((observable, previous, job) -> {
            if (job != null) {
                progressView.follow(job);   // stays on the last job when idle
            }
        });
        LogConsole.configure(logList);

        TrackTableConfigurer.configureTable(trackTable);
        TrackTableConfigurer.configureKeep(keepColumn);
        TrackTableConfigurer.configureIndex(indexColumn);
        TrackTableConfigurer.configureType(typeColumn);
        TrackTableConfigurer.configureCodec(codecColumn);
        TrackTableConfigurer.configureChannels(channelsColumn);
        TrackTableConfigurer.configureLanguage(languageColumn);
        TrackTableConfigurer.configureTitle(titleColumn);
        TrackTableConfigurer.configureAction(actionColumn);
        TrackTableConfigurer.configureDefault(defaultColumn);

        outputField.setEditable(false);
        configureDragAndDrop();
        updateControls();
        updateQueueHeader();
        updateTracksHeader();
    }

    @FXML
    private void onAddFiles() {

        FileChooser chooser = new FileChooser();
        chooser.setTitle(Messages.get("files.chooser.title"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                Messages.get("files.chooser.filter", String.join(", ", DroppedFiles.chooserPatterns())), DroppedFiles.chooserPatterns()));

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

    private void configureDragAndDrop() {

        rootPane.setOnDragOver(event -> {
            if (acceptsDrag(event)) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        rootPane.setOnDragEntered(event -> {
            rootPane.pseudoClassStateChanged(DRAG_OVER, acceptsDrag(event));
            event.consume();
        });
        rootPane.setOnDragExited(event -> {
            rootPane.pseudoClassStateChanged(DRAG_OVER, false);
            event.consume();
        });
        rootPane.setOnDragDropped(event -> {
            boolean added = event.getDragboard().hasFiles()
                    && dropFiles(event.getDragboard().getFiles().stream()
                    .map(File::toPath)
                    .toList());
            event.setDropCompleted(added);
            event.consume();
        });
    }

    private boolean acceptsDrag(DragEvent event) {

        return canAddFiles()
                && event.getDragboard().hasFiles()
                && DroppedFiles.mayContainFilms(event.getDragboard().getFiles().stream()
                .map(File::toPath)
                .toList());
    }

    boolean dropFiles(List<Path> dropped) {

        if (!canAddFiles()) {
            return false;
        }

        DroppedFiles files = DroppedFiles.of(dropped);
        if (!files.skipped().isEmpty()) {
            log.info("Skipped dropped files: {}", files.skipped());
            showWarning(Messages.get(files.supported().isEmpty() ? "files.noneSupported" : "files.someSkipped"),
                    Messages.get("files.skippedContent", String.join(", ", DroppedFiles.chooserPatterns()))
                            + System.lineSeparator() + fileNames(files.skipped()));
        }
        if (files.supported().isEmpty()) {
            return false;
        }

        lastDirectory = files.supported().getFirst().getParent().toFile();
        addFiles(files.supported());

        return true;
    }

    private boolean canAddFiles() {

        return !queue.isRunning() && !loading;
    }

    void addFiles(List<Path> files) {

        List<Path> alreadyQueued = files.stream().filter(this::isQueued).toList();
        List<Path> toAdd = files.stream().filter(file -> !isQueued(file)).toList();
        if (!alreadyQueued.isEmpty()) {
            showWarning(Messages.get("files.alreadyQueued"), fileNames(alreadyQueued));
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
                        failures.add(file.getFileName() + ": " + Messages.describe(e));
                    }
                }

                return failures;
            }
        };

        task.setOnSucceeded(event -> {
            setLoading(false);
            if (!task.getValue().isEmpty()) {
                showWarning(Messages.get("files.someUnreadable"), String.join(System.lineSeparator(), task.getValue()));
            }
        });
        task.setOnFailed(event -> {
            setLoading(false);
            showError(Messages.get("files.unreadable"), task.getException());
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
        job.statusProperty().addListener((observable, previous, status) -> logResult(job, status));
        job.appendLog(LogLines.versionLine(AppVersion.current(), ffmpegVersionLine));
        job.appendLog(Messages.get("log.loaded", info.path().getFileName(), Messages.count("tracks.count", info.streams().size())));

        queue.add(job);
        queueList.getSelectionModel().select(job);

        return job;
    }

    private static void logResult(ConversionJob job, JobStatus status) {

        switch (status) {
            case DONE -> job.appendLog(Messages.get("log.done", job.getOutput().getFileName()));
            case FAILED -> job.appendLog(Messages.get("log.failed", Messages.get(job.getError())));
            case CANCELLED -> job.appendLog(Messages.get("log.cancelled"));
            default -> { }
        }
    }

    private void showJob(ConversionJob job) {

        if (shown != null) {
            shown.statusProperty().removeListener(shownStatusListener);
            outputField.textProperty().unbind();
        }
        shown = job;

        if (job == null) {
            trackTable.getItems().clear();
            outputField.clear();
            logList.setItems(FXCollections.emptyObservableList());
        } else {
            trackTable.getItems().setAll(job.getPlans().stream()
                    .filter(plan -> TrackTableConfigurer.isShown(plan.getStream().type()))
                    .toList());
            outputField.textProperty().bind(Bindings.createStringBinding(
                    () -> job.getOutput().toString(), job.outputProperty()));
            logList.setItems(job.getLog());
            job.statusProperty().addListener(shownStatusListener);
        }

        updateTracksHeader();
        updateControls();
    }

    @FXML
    private void onChangeOutput() {

        ConversionJob job = shown;
        if (job == null || !job.isEditable()) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle(Messages.get("output.chooser.title"));
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
            showWarning(Messages.get("output.invalid"), Messages.get("output.sameAsSource"));
            return;
        }
        if (outputsInUse(job).stream().anyMatch(path -> OutputPathResolver.isSameFile(path, chosen))) {
            showWarning(Messages.get("output.invalid"), Messages.get("output.takenByOther"));
            return;
        }
        job.setOutput(chosen);
    }

    @FXML
    private void onApplyToAll() {

        if (shown != null && shown.isEditable()) {
            applyToAll(shown);
        }
    }

    /** Copies the track settings of one file to every other waiting file with the same track layout. */
    void applyToAll(ConversionJob from) {

        List<ConversionJob> targets = pendingJobs().stream().filter(job -> job != from).toList();
        if (targets.isEmpty()) {
            showInfo(Messages.get("tracks.applyToAll"), Messages.get("applyToAll.noOthers"));
            return;
        }

        int applied = 0;
        List<String> skipped = new ArrayList<>();
        for (ConversionJob job : targets) {
            Optional<UserMessage> difference = TrackPlans.layoutDifference(from.getPlans(), job.getPlans());
            if (difference.isPresent()) {
                skipped.add("• " + job.getInput().getFileName() + ": " + Messages.get(difference.get()));
            } else {
                TrackPlans.copySettings(from.getPlans(), job.getPlans());
                applied++;
            }
        }

        String result = Messages.count("applyToAll.count", applied);
        if (skipped.isEmpty()) {
            showInfo(Messages.get("tracks.applyToAll"), result);
        } else {
            showWarning(Messages.get("tracks.applyToAll"), result + System.lineSeparator()
                    + Messages.get("applyToAll.skipped") + System.lineSeparator()
                    + String.join(System.lineSeparator(), skipped));
        }
    }

    @FXML
    private void onOutputFolderForAll() {

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(Messages.get("output.folderForAll.title"));
        if (shown != null) {
            File current = shown.getOutput().getParent().toFile();
            if (current.isDirectory()) {
                chooser.setInitialDirectory(current);
            }
        }

        File folder = chooser.showDialog(window());
        if (folder != null) {
            moveOutputsTo(folder.toPath());
        }
    }

    /** Moves the outputs of all waiting files to the folder, keeping their names where possible. */
    void moveOutputsTo(Path folder) {

        List<String> renamed = new ArrayList<>();
        for (ConversionJob job : pendingJobs()) {
            List<Path> taken = new ArrayList<>(outputsInUse(job));
            taken.add(job.getInput());   // never overwrite a source film
            Path moved = OutputPathResolver.inFolder(job.getOutput(), folder, taken);
            if (!moved.getFileName().equals(job.getOutput().getFileName())) {
                renamed.add("• " + job.getOutput().getFileName() + " → " + moved.getFileName());
            }
            job.setOutput(moved);
        }

        if (!renamed.isEmpty()) {
            showInfo(Messages.get("output.folderForAll.header"), Messages.get("output.folderForAll.renamed")
                    + System.lineSeparator() + String.join(System.lineSeparator(), renamed));
        }
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
            showWarning(Messages.get("start.failed"), Messages.get("start.nothingWaiting"));
            return;
        }

        List<String> errors = new ArrayList<>();
        ConversionJob firstInvalid = null;
        for (ConversionJob job : pending) {
            if (job.getMode() == ConversionMode.QUICK) {
                continue;
            }
            List<PlanProblem> problems = TrackPlans.validate(job.getPlans());
            if (!problems.isEmpty() && firstInvalid == null) {
                firstInvalid = job;
            }
            problems.forEach(problem -> errors.add(job.getInput().getFileName() + ": " + Messages.get(problem.messageKey())));
        }
        if (firstInvalid != null) {
            queueList.getSelectionModel().select(firstInvalid);
            showWarning(Messages.get("start.failed"), String.join(System.lineSeparator(), errors));
            return;
        }

        startQueue(pending);
    }

    @FXML
    private void onQuickConvert() {

        List<ConversionJob> pending = pendingJobs();
        if (pending.isEmpty()) {
            showWarning(Messages.get("start.failed"), Messages.get("start.nothingWaiting"));
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
        lines.add(Messages.get("summary.done", done));
        if (!failed.isEmpty()) {
            lines.add(Messages.get("summary.failed", failed.size()));
        }
        if (cancelled > 0) {
            lines.add(Messages.get("summary.cancelled", cancelled));
        }
        for (ConversionJob job : failed) {
            lines.add("• " + job.getInput().getFileName() + ": " + firstLine(Messages.get(job.getError())));
        }
        String summary = String.join(System.lineSeparator(), lines);

        if (failed.isEmpty()) {
            showInfo(Messages.get("summary.finished"), summary);
        } else {
            showWarning(Messages.get("summary.finishedWithErrors"), summary + System.lineSeparator() + Messages.get("summary.details"));
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
            showError(Messages.get("about.openFailed"), e);
        }
    }

    private void updateControls() {

        boolean running = queue.isRunning();
        boolean editable = shown != null && shown.isEditable();

        addFilesButton.setDisable(!canAddFiles());
        ffmpegSettingsButton.setDisable(running);
        startButton.setDisable(running);
        quickConvertButton.setDisable(running);
        cancelButton.setDisable(!running);
        removeFromQueueButton.setDisable(shown == null || shown.getStatus() == JobStatus.RUNNING);
        trackTable.setDisable(shown != null && !editable);
        changeOutputButton.setDisable(!editable);
        applyToAllButton.setDisable(!editable);
        outputFolderButton.setDisable(pendingJobs().isEmpty());
    }

    private void updateQueueHeader() {

        List<ConversionJob> jobs = queue.getJobs();
        long remaining = jobs.stream().filter(job -> !job.getStatus().isFinished()).count();
        queueTitleLabel.setText(Messages.get("queue.title", jobs.size()));
        queueRemainingLabel.setText(Messages.get("queue.remaining", remaining));
    }

    private void updateTracksHeader() {

        if (shown == null) {
            tracksTitleLabel.setText(Messages.get("tracks.title"));
            tracksFileLabel.setText("");
            tracksFileLabel.setTooltip(null);
            tracksInfoLabel.setText("");
            trackCountLabel.setText("");
            return;
        }

        tracksTitleLabel.setText(Messages.get("tracks.titleWithFile"));
        tracksFileLabel.setText(shown.getInput().getFileName().toString());
        tracksFileLabel.setTooltip(new Tooltip(shown.getInput().toString()));
        tracksInfoLabel.setText(TextFormats.fileInfo(shown.getMediaInfo()));
        trackCountLabel.setText(Messages.count("tracks.count", trackTable.getItems().size()));
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

    private void showError(String header, Throwable error) {

        log.warn(header, error);
        showAlert(Alert.AlertType.ERROR, header, Messages.describe(error));
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
