package pl.audiofix.queue;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.audiofix.ffmpeg.CommandBuilder;
import pl.audiofix.ffmpeg.FfmpegListener;
import pl.audiofix.ffmpeg.FfmpegRunner;
import pl.audiofix.model.ConversionJob;
import pl.audiofix.model.JobStatus;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;


public class JobQueue {

    private static final Logger log = LoggerFactory.getLogger(JobQueue.class);
    private static final long SHUTDOWN_WAIT_SECONDS = 15;

    private final ObservableList<ConversionJob> jobs =
            FXCollections.observableArrayList(job -> new Observable[]{job.statusProperty()});
    private final ReadOnlyObjectWrapper<ConversionJob> currentJob = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyBooleanWrapper running = new ReadOnlyBooleanWrapper();
    private final Function<ConversionJob, List<String>> commands;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().name("conversion-queue").daemon().factory());

    private Active active;
    private boolean shuttingDown;

    public JobQueue(Function<ConversionJob, List<String>> commands) {
        this.commands = commands;
    }

    public static JobQueue withFfmpeg(Supplier<Path> ffmpeg) {
        CommandBuilder builder = new CommandBuilder();
        return new JobQueue(job -> builder.build(ffmpeg.get(), job));
    }

    public ObservableList<ConversionJob> getJobs() {
        return jobs;
    }

    public ReadOnlyBooleanProperty runningProperty() {
        return running.getReadOnlyProperty();
    }

    public boolean isRunning() {
        return running.get();
    }

    public void add(ConversionJob job) {
        requireFxThread();
        jobs.add(job);
    }

    public boolean remove(ConversionJob job) {
        requireFxThread();
        if (job.getStatus() == JobStatus.RUNNING) {
            return false;
        }
        return jobs.remove(job);
    }

    public void start() {
        requireFxThread();
        if (running.get() || shuttingDown) {
            return;
        }
        running.set(true);
        worker.execute(this::processPending);
    }

    public void cancelCurrent() {
        requireFxThread();
        if (active != null) {
            active.runner().cancel();
        }
    }

    public void shutdown() {
        requireFxThread();
        shuttingDown = true;
        cancelCurrent();
        worker.shutdownNow();
        try {
            if (!worker.awaitTermination(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS)) {
                log.warn("Conversion did not stop within {} s", SHUTDOWN_WAIT_SECONDS);
            }
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    public ReadOnlyObjectProperty<ConversionJob> currentJobProperty() {

        return currentJob.getReadOnlyProperty();
    }

    private void processPending() {
        while (true) {
            FfmpegRunner runner = new FfmpegRunner();
            Optional<Next> next;
            try {
                next = callOnFxThread(() -> takeNext(runner));
            } catch (InterruptedException _) {
                return;   // shutdown
            }
            if (next.isEmpty()) {
                return;
            }
            convert(next.get(), runner);
        }
    }

    private void convert(Next next, FfmpegRunner runner) {
        ConversionJob job = next.job();
        try {
            runner.run(next.command(), next.output(), next.durationSec(), new FfmpegListener() {
                @Override
                public void onProgress(double fraction) {
                    onFxThread(() -> job.setProgress(fraction));
                }

                @Override
                public void onLog(String line) {
                    onFxThread(() -> job.appendLog(line));
                }
            });
            onFxThread(() -> finish(job, JobStatus.DONE, null));
        } catch (CancellationException _) {
            onFxThread(() -> finish(job, JobStatus.CANCELLED, null));
        } catch (RuntimeException e) {
            log.warn("Conversion of {} failed", job.getInput(), e);
            onFxThread(() -> finish(job, JobStatus.FAILED, e.getMessage()));
        }
    }

    private Optional<Next> takeNext(FfmpegRunner runner) {

        if (shuttingDown) {
            running.set(false);
            return Optional.empty();
        }
        for (ConversionJob job : jobs) {
            if (job.getStatus() != JobStatus.PENDING) {
                continue;
            }
            job.markRunning();
            List<String> command;
            try {
                command = commands.apply(job);
            } catch (RuntimeException e) {
                log.warn("Cannot build command for {}", job.getInput(), e);
                job.markFailed(e.getMessage());
                continue;
            }
            job.appendLog(CommandBuilder.toCommandLine(command));
            active = new Active(job, runner);
            currentJob.set(job);

            return Optional.of(new Next(job, command, job.getOutput(), job.getMediaInfo().durationSec()));
        }
        running.set(false);
        return Optional.empty();
    }

    private void finish(ConversionJob job, JobStatus result, String errorMessage) {

        active = null;
        currentJob.set(null);

        switch (result) {
            case DONE -> job.markDone();
            case FAILED -> job.markFailed(errorMessage);
            case CANCELLED -> job.markCancelled();
            default -> throw new IllegalArgumentException("Not a result: " + result);
        }
    }

    private static void requireFxThread() {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("JobQueue must be used on the JavaFX Application Thread");
        }
    }

    private static void onFxThread(Runnable action) {
        try {
            Platform.runLater(action);
        } catch (IllegalStateException e) {
            log.debug("JavaFX already stopped, update dropped", e);
        }
    }

    private static <T> T callOnFxThread(Supplier<T> action) throws InterruptedException {
        CompletableFuture<T> result = new CompletableFuture<>();
        onFxThread(() -> {
            try {
                result.complete(action.get());
            } catch (RuntimeException e) {
                result.completeExceptionally(e);
            }
        });
        try {
            return result.get();
        } catch (ExecutionException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    private record Active(ConversionJob job, FfmpegRunner runner) {
    }

    private record Next(ConversionJob job, List<String> command, Path output, double durationSec) {
    }
}