package pl.audiofix.ffmpeg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

public class FfmpegLocator {

    private static final Logger log = LoggerFactory.getLogger(FfmpegLocator.class);

    static final String FFMPEG_EXE = "ffmpeg.exe";
    private static final String PREF_KEY = "ffmpegPath";

    private final Preferences preferences;
    private final String pathEnv;

    public FfmpegLocator() {

        this(Preferences.userNodeForPackage(FfmpegLocator.class), System.getenv("PATH"));
    }

    FfmpegLocator(Preferences preferences, String pathEnv) {

        this.preferences = preferences;
        this.pathEnv = pathEnv;
    }

    public Optional<FfmpegPaths> locate() {

        String preferencePath = preferences.get(PREF_KEY, null);

        if (preferencePath != null) {

            try {
                Path ffmpegPath = Path.of(preferencePath);

                FfmpegPaths paths = FfmpegPaths.fromFfmpeg(ffmpegPath);

                if (isComplete(paths)) {

                    log.info("Using saved ffmpeg: {}", paths.ffmpeg());
                    return Optional.of(paths);
                }
                log.warn("Saved ffmpeg is missing or incomplete, clearing: {}", preferencePath);
            } catch (InvalidPathException _) {

                log.warn("Invalid saved ffmpeg path, clearing: {}", preferencePath);
            }

            preferences.remove(PREF_KEY);
        }

        if (pathEnv == null) {

            return Optional.empty();
        }

        for (String path : pathEnv.split(File.pathSeparator)) {

            String pathCleaned = path.trim().replace("\"", "");

            if (pathCleaned.isEmpty()) {
                continue;
            }

            try {
                Path ffmpegPath = Path.of(pathCleaned, FFMPEG_EXE);

                FfmpegPaths paths = FfmpegPaths.fromFfmpeg(ffmpegPath);
                if (isComplete(paths)) {

                    log.info("Using ffmpeg from PATH: {}", paths.ffmpeg());
                    return Optional.of(paths);
                }
            } catch (InvalidPathException _) {

                log.debug("Skipping invalid PATH entry: {}", pathCleaned);
            }
        }

        log.info("ffmpeg not found in preferences or PATH");
        return Optional.empty();
    }

    public FfmpegPaths save(Path ffmpegPath) {

        if (!Files.isRegularFile(ffmpegPath)
                || !ffmpegPath.getFileName().toString().equalsIgnoreCase(FFMPEG_EXE)) {
            throw new IllegalArgumentException("Wskazany plik to nie ffmpeg.exe: " + ffmpegPath);
        }

        FfmpegPaths paths = FfmpegPaths.fromFfmpeg(ffmpegPath);
        if (!Files.isRegularFile(paths.ffprobe())) {
            throw new IllegalArgumentException("Nie znaleziono ffprobe.exe w folderze: " + ffmpegPath.getParent());
        }

        if (!ffmpegVersionCheckOk(ffmpegPath)) {
            throw new IllegalArgumentException("Nie można uruchomić ffmpeg.exe lub zwraca niepoprawny kod wyjścia.");
        }

        preferences.put(PREF_KEY, ffmpegPath.toAbsolutePath().toString());
        log.info("Saved ffmpeg path: {}", ffmpegPath.toAbsolutePath());

        return paths;
    }

    boolean ffmpegVersionCheckOk(Path ffmpegPath) {

        try {
            Process p = new ProcessBuilder(ffmpegPath.toString(), "-version")
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();

            boolean finished = p.waitFor(5, TimeUnit.SECONDS);

            if (!finished) {
                p.destroyForcibly();
                log.warn("ffmpeg -version timed out: {}", ffmpegPath);
                return false;
            }

            int exitCode = p.exitValue();
            if (exitCode != 0) {
                log.warn("ffmpeg -version exited with code {}: {}", exitCode, ffmpegPath);
            }
            return exitCode == 0;
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.warn("Cannot run ffmpeg -version: {}", ffmpegPath, e);
            return false;
        }
    }

    private static boolean isComplete(FfmpegPaths paths) {

        return Files.isRegularFile(paths.ffmpeg()) && Files.isRegularFile(paths.ffprobe());
    }
}
