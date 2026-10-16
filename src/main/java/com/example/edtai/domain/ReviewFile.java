package com.example.edtai.domain;

import java.util.List;

public record ReviewFile(String path, List<DiffHunk> hunks, List<String> lines, ReviewBaseline.Kind baseline) {

    public ReviewFile {
        hunks = List.copyOf(hunks);
        lines = List.copyOf(lines);
    }

    public static ReviewFile of(ReviewEntry entry) {
        // Собирает сведения о правках файла
        return new ReviewFile(entry.path(), entry.hunks(), TextShape.of(entry.aiText()).lines(),
                entry.baseline().kind());
    }

    public int count(ChangeKind kind) {
        // Считает строки изменений одного вида
        return hunks.stream().filter(hunk -> hunk.kind() == kind)
                .mapToInt(hunk -> kind == ChangeKind.DELETED ? hunk.oldLines().size() : hunk.newCount()).sum();
    }

    public boolean revertible() {
        // Проверяет, можно ли откатить файл
        return baseline != ReviewBaseline.Kind.UNKNOWN;
    }
}
