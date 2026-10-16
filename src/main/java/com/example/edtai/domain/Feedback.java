package com.example.edtai.domain;

import java.util.List;

public record Feedback(
        FeedbackKind kind,
        String target,
        String path,
        int startLine,
        int endLine,
        List<String> categories,
        String comment,
        String originalCode,
        String currentCode,
        AiProvenance provenance,
        String fragmentId) {

    public Feedback {
        categories = List.copyOf(categories);
        path = path == null ? "" : path;
        comment = comment == null ? "" : comment;
        originalCode = originalCode == null ? "" : originalCode;
        currentCode = currentCode == null ? "" : currentCode;
        fragmentId = fragmentId == null ? "" : fragmentId;
    }
}
