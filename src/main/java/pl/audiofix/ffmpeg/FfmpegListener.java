package pl.audiofix.ffmpeg;

public interface FfmpegListener {

    default void onProgress(double fraction) {

    }

    default void onLog(String line) {

    }
}
