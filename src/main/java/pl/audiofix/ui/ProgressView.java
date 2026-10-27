package pl.audiofix.ui;

import javafx.beans.InvalidationListener;
import javafx.css.PseudoClass;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import pl.audiofix.model.ConversionJob;
import pl.audiofix.model.JobStatus;

import java.util.function.LongSupplier;

/**
 * Bottom bar progress of the file being converted: the bar, what is converted, the percent and time left.
 * After the file ends it shows how it ended, until the next file starts.
 */
final class ProgressView {

    /** Orange bar while ffmpeg re-encodes audio, accent while it only copies tracks. */
    static final PseudoClass CONVERTING = PseudoClass.getPseudoClass("converting");

    private final ProgressBar bar;
    private final Label status;
    private final Label percent;
    private final LongSupplier nanoTime;
    private final InvalidationListener jobListener = observable -> update();

    private ConversionJob job;
    private EtaEstimator eta;

    ProgressView(ProgressBar bar, Label status, Label percent, LongSupplier nanoTime) {

        this.bar = bar;
        this.status = status;
        this.percent = percent;
        this.nanoTime = nanoTime;
        status.setText("");
        percent.setText("");
    }

    /** Starts showing the job that has just started running. */
    void follow(ConversionJob next) {

        if (job != null) {
            job.progressProperty().removeListener(jobListener);
            job.statusProperty().removeListener(jobListener);
        }

        job = next;
        eta = new EtaEstimator(nanoTime);
        job.progressProperty().addListener(jobListener);
        job.statusProperty().addListener(jobListener);
        bar.pseudoClassStateChanged(CONVERTING, job.convertsAudio());
        update();
    }

    void refresh() {

        if (job != null) {
            update();
        }
    }

    private void update() {

        boolean running = job.getStatus() == JobStatus.RUNNING;
        status.setText(running ? ProgressTexts.running(job) : ProgressTexts.finished(job.getStatus()));

        if (!running) {
            bar.setProgress(job.getProgress());
            percent.setText("");
        } else if (job.getMediaInfo().durationSec() <= 0) {
            // ffmpeg reports time, not percent: without the film's length there is nothing to divide by
            bar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            percent.setText("");
        } else {
            bar.setProgress(job.getProgress());
            percent.setText(ProgressTexts.percent(job.getProgress(), eta.remaining(job.getProgress())));
        }
    }
}
