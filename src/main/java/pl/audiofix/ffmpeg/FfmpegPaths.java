package pl.audiofix.ffmpeg;

import java.nio.file.Path;

public record FfmpegPaths(Path ffmpeg, Path ffprobe) {

    public static FfmpegPaths fromFfmpeg(Path ffmpeg) {

        Path ffprobe = ffmpeg.resolveSibling("ffprobe.exe");

        return new FfmpegPaths(ffmpeg, ffprobe);
    }
}
