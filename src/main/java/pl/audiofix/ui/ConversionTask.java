package pl.audiofix.ui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import pl.audiofix.ffmpeg.FfmpegListener;
import pl.audiofix.ffmpeg.FfmpegRunner;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;


class ConversionTask extends Task<Void> {

    private final FfmpegRunner runner = new FfmpegRunner();
    private final List<String> command;
    private final Path output;
    private final double durationSec;
    private final Consumer<String> log;

    ConversionTask(List<String> command, Path output, double durationSec, Consumer<String> log) {

        this.command = command;
        this.output = output;
        this.durationSec = durationSec;
        this.log = log;
    }

    Path output() {

        return output;
    }

    void stopNow() {

        runner.cancel();
        cancel();
    }

    @Override
    protected Void call() {

        runner.run(command, output, durationSec, new FfmpegListener() {
            @Override
            public void onProgress(double fraction) {
                updateProgress(fraction, 1.0);
            }

            @Override
            public void onLog(String line) {
                Platform.runLater(() -> log.accept(line));
            }
        });

        return null;
    }
}
