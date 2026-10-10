package pl.audiofix.ui;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import pl.audiofix.model.ConversionJob;
import pl.audiofix.model.ConversionMode;
import pl.audiofix.model.JobStatus;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;


final class QueueCell extends ListCell<ConversionJob> {

    static final List<String> STATUS_CLASSES = Arrays.stream(JobStatus.values()).map(QueueCell::statusClass).toList();

    private final Label name = new Label();
    private final Label status = new Label();
    private final ProgressBar progress = new ProgressBar();
    private final VBox content = new VBox(2, name, status, progress);
    private final InvalidationListener refresh = observable -> refresh();

    private ConversionJob shown;

    QueueCell() {

        getStyleClass().add("queue-cell");
        name.getStyleClass().add("queue-name");
        status.getStyleClass().add("queue-status");
        progress.getStyleClass().add("queue-progress");
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.managedProperty().bind(progress.visibleProperty());   // hidden bar takes no space
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
        status.textProperty().bind(Bindings.createStringBinding(
                () -> statusText(job.getStatus(), job.getProgress(), job.getMode()),
                job.statusProperty(), job.progressProperty(), job.modeProperty()));
        progress.progressProperty().bind(job.progressProperty());
        job.statusProperty().addListener(refresh);
        job.errorMessageProperty().addListener(refresh);   // markFailed sets it after the status
        refresh();
        setGraphic(content);
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