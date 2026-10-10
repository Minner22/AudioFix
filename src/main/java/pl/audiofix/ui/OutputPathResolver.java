package pl.audiofix.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class OutputPathResolver {

    static final String SUFFIX = "_fixed";
    static final String EXTENSION = ".mkv";

    private OutputPathResolver() {
    }

    public static Path defaultOutput(Path input) {

        return defaultOutput(input, List.of());
    }

    public static Path defaultOutput(Path input, Collection<Path> taken) {

        String base = withoutExtension(input.getFileName().toString()) + SUFFIX;
        Path dir = input.toAbsolutePath().getParent();

        Path candidate = dir.resolve(base + EXTENSION);

        for (int n = 1; Files.exists(candidate) || isTaken(candidate, taken); n++) {
            candidate = dir.resolve(base + " (" + n + ")" + EXTENSION);
        }

        return candidate;
    }

    public static Path withMkvExtension(Path chosen) {

        String name = chosen.getFileName().toString();

        return name.toLowerCase(Locale.ROOT).endsWith(EXTENSION)
                ? chosen
                : chosen.resolveSibling(name + EXTENSION);
    }

    public static boolean isSameFile(Path a, Path b) {

        return a.toAbsolutePath().normalize().toString()
                .equalsIgnoreCase(b.toAbsolutePath().normalize().toString());
    }

    public static Path inFolder(Path output, Path folder, Collection<Path> taken) {

        String name = output.getFileName().toString();
        String base = withoutExtension(name);
        String extension = name.substring(base.length());

        Path candidate = folder.resolve(name);
        for (int n = 1; Files.exists(candidate) || isTaken(candidate, taken); n++) {
            candidate = folder.resolve(base + " (" + n + ")" + extension);
        }

        return candidate;
    }

    private static String withoutExtension(String name) {

        int dot = name.lastIndexOf('.');

        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static boolean isTaken(Path candidate, Collection<Path> taken) {

        return taken.stream().anyMatch(path -> isSameFile(path, candidate));
    }
}
