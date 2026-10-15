package com.example.edtai.infrastructure.process;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public final class ExecutableLocator {

    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");

    private final List<Path> directories;

    public ExecutableLocator() {
        this.directories = collectDirectories();
    }

    public Optional<Path> find(String name) {
        // Ищет исполняемый файл по имени
        for (Path directory : directories) {
            for (String candidate : candidates(name)) {
                Path file = directory.resolve(candidate);
                if (Files.isRegularFile(file) && (WINDOWS || Files.isExecutable(file))) {
                    return Optional.of(file);
                }
            }
        }
        return Optional.empty();
    }

    public String searchPath() {
        // Собирает расширенную переменную PATH
        return String.join(File.pathSeparator, directories.stream().map(Path::toString).toList());
    }

    private static List<String> candidates(String name) {
        // Возвращает варианты имени файла
        if (!WINDOWS) {
            return List.of(name);
        }
        return List.of(name + ".cmd", name + ".exe", name + ".bat", name);
    }

    private static List<Path> collectDirectories() {
        // Собирает каталоги для поиска
        Set<Path> result = new LinkedHashSet<>();
        Path home = Path.of(System.getProperty("user.home"));
        result.add(home.resolve(".local").resolve("bin"));
        result.add(home.resolve(".bun").resolve("bin"));
        result.add(home.resolve(".npm-global").resolve("bin"));
        result.addAll(nvmDirectories(home));
        if (WINDOWS) {
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                result.add(Path.of(appData, "npm"));
            }
        } else {
            result.add(Path.of("/usr/local/bin"));
            result.add(Path.of("/opt/homebrew/bin"));
        }
        String path = System.getenv("PATH");
        if (path != null) {
            for (String entry : path.split(File.pathSeparator)) {
                addSafely(result, entry);
            }
        }
        return new ArrayList<>(result);
    }

    private static void addSafely(Set<Path> result, String entry) {
        // Добавляет каталог, пропуская некорректные пути
        try {
            if (!entry.isBlank()) {
                result.add(Path.of(entry.strip()));
            }
        } catch (InvalidPathException ignored) {
            return;
        }
    }

    private static List<Path> nvmDirectories(Path home) {
        // Находит версии node из nvm
        Path versions = home.resolve(".nvm").resolve("versions").resolve("node");
        if (!Files.isDirectory(versions)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(versions)) {
            return stream.sorted(Comparator.comparing(Path::getFileName).reversed())
                    .map(version -> version.resolve("bin"))
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }
}
