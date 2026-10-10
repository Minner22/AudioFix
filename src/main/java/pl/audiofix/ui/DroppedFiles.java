package pl.audiofix.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

public record DroppedFiles(List<Path> supported, List<Path> skipped) {

    public static final List<String> EXTENSIONS = List.of("mkv", "mp4", "m2ts");

    private static final Comparator<Path> BY_NAME = Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT));

    public DroppedFiles {

        supported = List.copyOf(supported);
        skipped = List.copyOf(skipped);
    }

    public static DroppedFiles of(List<Path> dropped) {

        Set<Path> supported = new LinkedHashSet<>();
        List<Path> skipped = new ArrayList<>();
        for (Path path : dropped) {
            if (Files.isDirectory(path)) {
                List<Path> films = filmsIn(path);
                if (films.isEmpty()) {
                    skipped.add(path);
                }
                supported.addAll(films);
            } else if (isSupported(path)) {
                supported.add(path);
            } else {
                skipped.add(path);
            }
        }

        return new DroppedFiles(new ArrayList<>(supported), skipped);
    }

    public static boolean mayContainFilms(List<Path> dragged) {

        return dragged.stream().anyMatch(path -> isSupported(path) || Files.isDirectory(path));
    }

    public static boolean isSupported(Path file) {

        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return EXTENSIONS.stream().anyMatch(extension -> name.endsWith("." + extension));
    }

    public static List<String> chooserPatterns() {

        return EXTENSIONS.stream()
                .map(extension -> "*." + extension)
                .toList();
    }

    public static List<Path> filmsIn(Path folder) {

        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(Files::isRegularFile)
                    .filter(DroppedFiles::isSupported)
                    .sorted(BY_NAME)
                    .toList();
        } catch (IOException _) {
            return List.of();
        }
    }
}
