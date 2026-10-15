package com.example.edtai.infrastructure.files;

import com.example.edtai.application.port.FileFinder;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class WorkspaceFiles implements FileFinder {

    private static final Set<String> SKIPPED = Set.of(".git", ".metadata", ".settings", ".idea", ".vscode",
            "node_modules", "target", "bin", "dist", "build", ".gradle", ".mvn");
    private static final int MAX_FILES = 60_000;
    private static final long TTL_MS = 30_000;

    private final Path root;
    private List<String> index = List.of();
    private long indexedAt;

    public WorkspaceFiles(Path root) {
        this.root = root;
    }

    @Override
    public synchronized List<String> find(String query, int limit) {
        // Ищет файлы по части имени или пути
        String needle = query.toLowerCase(Locale.ROOT).replace('\\', '/');
        return paths().stream()
                .map(path -> new Match(path, FileScore.of(path, needle)))
                .filter(match -> match.score() > 0)
                .sorted(Comparator.comparingInt(Match::score).reversed()
                        .thenComparingInt(match -> match.path().length()))
                .limit(limit)
                .map(Match::path)
                .toList();
    }

    private List<String> paths() {
        // Возвращает кэшированный список файлов
        if (System.currentTimeMillis() - indexedAt > TTL_MS) {
            index = scan();
            indexedAt = System.currentTimeMillis();
        }
        return index;
    }

    private List<String> scan() {
        // Обходит рабочий каталог агента
        List<String> found = new ArrayList<>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // Пропускает служебные каталоги
                    boolean skip = !dir.equals(root) && SKIPPED.contains(dir.getFileName().toString());
                    return skip ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    // Запоминает относительный путь файла
                    found.add(root.relativize(file).toString().replace('\\', '/'));
                    return found.size() >= MAX_FILES ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException error) {
                    // Пропускает недоступные файлы
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать файлы рабочего каталога", e);
        }
        return found;
    }

    private record Match(String path, int score) {
    }
}
