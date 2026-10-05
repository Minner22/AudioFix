package pl.audiofix.ffmpeg;

/**
 * Callbacks from {@link FfmpegRunner}.
 * <p>
 * Called from background threads, not from the JavaFX Application Thread -
 * UI updates have to go through {@code Platform.runLater} (or {@code Task.updateProgress}).
 */
public interface FfmpegListener {

    /** @param fraction conversion progress from 0.0 to 1.0 */
    default void onProgress(double fraction) {

    }

    /** @param line one line of ffmpeg stderr output */
    default void onLog(String line) {

    }
}
