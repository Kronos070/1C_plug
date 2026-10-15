package com.example.edtai.domain;

import java.util.List;

public record ReviewEntry(String path, ReviewBaseline baseline, String aiText) {

    public ReviewEntry {
        aiText = aiText == null ? "" : aiText;
    }

    public ReviewEntry withAiText(String text) {
        // Возвращает запись с новым текстом агента
        return new ReviewEntry(path, baseline, text);
    }

    public List<DiffHunk> hunks() {
        // Сравнивает исходник с текстом агента
        List<String> current = TextShape.of(aiText).lines();
        if (baseline.kind() == ReviewBaseline.Kind.TEXT) {
            return LineDiff.diff(TextShape.of(baseline.text()).lines(), current);
        }
        return current.isEmpty() ? List.of() : List.of(new DiffHunk(0, List.of(), 0, current.size()));
    }
}
