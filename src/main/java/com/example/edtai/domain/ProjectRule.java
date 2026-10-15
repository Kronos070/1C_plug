package com.example.edtai.domain;

public record ProjectRule(RuleScope scope, String target, String text) {

    public ProjectRule {
        target = target == null ? "" : target.replace('\\', '/');
        text = text == null ? "" : text.strip();
    }

    public boolean appliesTo(String path) {
        // Проверяет, относится ли правило к пути
        String normalized = path == null ? "" : path.replace('\\', '/');
        return switch (scope) {
            case CONFIG -> true;
            case MODULE -> !target.isEmpty() && normalized.endsWith(target);
            case FOLDER -> !target.isEmpty() && normalized.contains(target);
        };
    }
}
