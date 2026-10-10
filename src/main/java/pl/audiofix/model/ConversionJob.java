package pl.audiofix.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class ConversionJob {

    private final MediaInfo mediaInfo;
    private final List<TrackPlan> plans;
    private final ObjectProperty<ConversionMode> mode;
    private final ObjectProperty<Path> output;
    private final ReadOnlyObjectWrapper<JobStatus> status = new ReadOnlyObjectWrapper<>(JobStatus.PENDING);
    private final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper();
    private final ReadOnlyStringWrapper errorMessage = new ReadOnlyStringWrapper();
    private final ObservableList<String> log = FXCollections.observableArrayList();
    private final ObservableList<String> readOnlyLog = FXCollections.unmodifiableObservableList(log);

    public ConversionJob(MediaInfo mediaInfo, List<TrackPlan> plans, Path output, ConversionMode mode) {

        Objects.requireNonNull(mediaInfo, "mediaInfo should not be null");
        Objects.requireNonNull(output, "output should not be null");
        Objects.requireNonNull(mode, "mode should not be null");

        this.mediaInfo = mediaInfo;
        this.plans = List.copyOf(plans);
        this.mode = new  SimpleObjectProperty<>(mode);
        this.output = new SimpleObjectProperty<>(output);
    }

    public ReadOnlyObjectProperty<JobStatus> statusProperty() {

        return status.getReadOnlyProperty();
    }

    public ReadOnlyDoubleProperty progressProperty() {

        return progress.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty errorMessageProperty() {

        return errorMessage.getReadOnlyProperty();
    }

    public ReadOnlyObjectProperty<Path> outputProperty() {

        return output;
    }

    public Path getInput() {

        return mediaInfo.path();
    }

    public MediaInfo getMediaInfo() {

        return mediaInfo;
    }

    public List<TrackPlan> getPlans() {

        return plans;
    }

    public ReadOnlyObjectProperty<ConversionMode> modeProperty() {

        return mode;
    }

    public ConversionMode getMode() {

        return mode.get();
    }

    public Path getOutput() {

        return output.get();
    }

    public JobStatus getStatus() {

        return status.get();
    }

    public double getProgress() {

        return progress.get();
    }

    public String getErrorMessage() {

        return errorMessage.get();
    }

    public ObservableList<String> getLog() {

        return readOnlyLog;
    }

    public boolean isEditable() {

        return getStatus() == JobStatus.PENDING;
    }

    public void setOutput(Path value) {

        if (!isEditable()) {
            throw new IllegalStateException("Job is " + getStatus() + ", it cannot be changed");
        }
        output.set(Objects.requireNonNull(value));
    }

    public void markRunning() {

        transition(JobStatus.PENDING, JobStatus.RUNNING);
        progress.set(0);
    }

    public void markDone() {

        transition(JobStatus.RUNNING, JobStatus.DONE);
        progress.set(1);
    }

    public void markFailed(String message) {

        transition(JobStatus.RUNNING, JobStatus.FAILED);
        errorMessage.set(message);
        progress.set(0);
    }

    public void markCancelled() {

        JobStatus currentStatus = getStatus();

        if (currentStatus != JobStatus.PENDING && currentStatus != JobStatus.RUNNING) {
            throw new IllegalStateException("Cannot cancel a job that is " + currentStatus);
        }

        status.set(JobStatus.CANCELLED);
        progress.set(0);
    }

    public void setProgress(double fraction) {

        progress.set(Math.clamp(fraction, 0.0, 1.0));
    }

    public void setMode(ConversionMode value) {

        if (!isEditable()) {
            throw new IllegalStateException("Job is  " + getStatus() + ", it cannot be changed");
        }

        mode.set(Objects.requireNonNull(value));
    }

    public void appendLog(String line) {

        log.add(line);
    }

    private void transition(JobStatus from, JobStatus to) {

        if (getStatus() != from) {
            throw new IllegalStateException("Cannot go from " + getStatus() + " to " + to);
        }

        status.set(to);
    }

    @Override
    public String toString() {

        return mediaInfo.path().getFileName() + " (" + getStatus() + ")";
    }
}
