package pl.audiofix.ui;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import pl.audiofix.model.ConversionJob;
import pl.audiofix.model.ConversionMode;
import pl.audiofix.model.JobStatus;
import pl.audiofix.model.MediaInfo;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;


final class QueueCell extends ListCell<ConversionJob> {

    static final List<String> STATUS_CLASSES = Arrays.stream(JobStatus.values()).map(QueueCell::statusClass).toList();
    private static final Locale POLISH = Locale.forLanguageTag("pl-PL");
    private static final List<String> UNITS = List.of("B", "KB", "MB", "GB", "TB");

    private final Label name = new Label();
    private final Label details = new Label();
    private final Label status = new Label();
    private final Region spacer = new Region();
    private final HBox info = new HBox(8, details, spacer, status);
    private final ProgressBar progress = new ProgressBar();
    private final VBox content = new VBox(3, name, info, progress);
    private final InvalidationListener refresh = observable -> refresh();

    private ConversionJob shown;

    QueueCell() {

        getStyleClass().add("queue-cell");
        name.getStyleClass().add("queue-name");
        status.getStyleClass().add("queue-status");
        progress.getStyleClass().add("queue-progress");
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.managedProperty().bind(progress.visibleProperty());   // hidden bar takes no space

        details.getStyleClass().add("queue-details");

        setPrefWidth(0);
        name.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);   // keeps the end of the name, e.g. "REMUX.mkv"
        name.setEllipsisString("…");
        status.setMinWidth(Region.USE_PREF_SIZE);            // the badge is never cut
        HBox.setHgrow(spacer, Priority.ALWAYS);
        info.setAlignment(Pos.CENTER_LEFT);
    }

    @Override
    protected void updateItem(ConversionJob job, boolean empty) {

        super.updateItem(job, empty);
        stopShowing();

        if (empty || job == null) {
            setGraphic(null);
            setTooltip(null);
            return;
        }

        shown = job;
        name.setText(job.getInput().getFileName().toString());
        details.setText(detailsText(job.getMediaInfo()));
        status.textProperty().bind(Bindings.createStringBinding(
                () -> statusText(job.getStatus(), job.getProgress(), job.getMode()),
                job.statusProperty(), job.progressProperty(), job.modeProperty()));
        progress.progressProperty().bind(job.progressProperty());
        job.statusProperty().addListener(refresh);
        job.errorMessageProperty().addListener(refresh);   // markFailed sets it after the status
        refresh();
        setGraphic(content);
    }

    static String detailsText(MediaInfo media) {

        String size = media.sizeBytes() > 0 ? sizeText(media.sizeBytes()) : "";

        return Stream.of(size, containerText(media.path()))
                .filter(part -> !part.isEmpty())
                .collect(Collectors.joining(" • "));
    }

    static String sizeText(long bytes) {

        if (bytes < 1024) {
            return bytes + " B";
        }

        double value = bytes;
        int unit = 0;
        while (value >= 1024 && unit < UNITS.size() - 1) {
            value /= 1024;
            unit++;
        }

        return String.format(POLISH, "%.1f %s", value, UNITS.get(unit));
    }

    static String containerText(Path file) {

        String fileName = file.getFileName().toString();
        int dot = fileName.lastIndexOf('.');

        return dot > 0 ? fileName.substring(dot + 1).toUpperCase(Locale.ROOT) : "";
    }

    static String statusText(JobStatus status, double progress, ConversionMode mode) {

        String text = switch (status) {
            case PENDING -> "Oczekuje";
            case RUNNING -> "W trakcie " + Math.round(progress * 100) + "%";
            case DONE -> "Gotowe";
            case FAILED -> "Błąd";
            case CANCELLED -> "Anulowano";
        };

        return mode == ConversionMode.QUICK ? text + " · szybka" : text;
    }

    static String statusClass(JobStatus status) {

        return "status-" + status.name().toLowerCase(Locale.ROOT);
    }

    private void refresh() {

        JobStatus current = shown.getStatus();
        status.getStyleClass().removeAll(STATUS_CLASSES);
        status.getStyleClass().add(statusClass(current));
        progress.setVisible(current == JobStatus.RUNNING);

        String tip = current == JobStatus.FAILED && shown.getErrorMessage() != null
                ? shown.getInput() + "\n\n" + shown.getErrorMessage()
                : shown.getInput().toString();
        setTooltip(new Tooltip(tip));
    }

    private void stopShowing() {

        if (shown != null) {
            shown.statusProperty().removeListener(refresh);
            shown.errorMessageProperty().removeListener(refresh);
            status.textProperty().unbind();
            progress.progressProperty().unbind();
            shown = null;
        }
    }
}