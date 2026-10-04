package pl.audiofix.ffmpeg;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

public class FfmpegLocator {

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

            Path ffmpegPath = Path.of(preferencePath);

            FfmpegPaths paths = FfmpegPaths.fromFfmpeg(ffmpegPath);

            if (isComplete(paths)) {

                return Optional.of(paths);
            }
            else {

                preferences.remove(PREF_KEY);
            }
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

                    return Optional.of(paths);
                }
            } catch (InvalidPathException _) {

                System.err.println("Invalid path: " + pathCleaned);
            }
        }

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

        return FfmpegPaths.fromFfmpeg(ffmpegPath);
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
                return false;
            }

            return p.exitValue() == 0;
        } catch (Exception _) {
            return false;
        }
    }

    private static boolean isComplete(FfmpegPaths paths) {

        return Files.isRegularFile(paths.ffmpeg()) && Files.isRegularFile(paths.ffprobe());
    }
}
