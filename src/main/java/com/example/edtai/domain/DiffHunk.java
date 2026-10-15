package com.example.edtai.domain;

import java.util.List;

public record DiffHunk(int oldStart, List<String> oldLines, int newStart, int newCount) {

    public DiffHunk {
        oldLines = oldLines == null ? List.of() : List.copyOf(oldLines);
    }

    public ChangeKind kind() {
        // Определяет вид изменения блока
        if (oldLines.isEmpty()) {
            return ChangeKind.ADDED;
        }
        return newCount == 0 ? ChangeKind.DELETED : ChangeKind.MODIFIED;
    }
}
