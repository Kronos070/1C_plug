package com.example.edtai.domain;

import java.util.Arrays;
import java.util.Optional;

public enum FeedbackKind {
    LIKE("like", "Нравится"),
    DISLIKE("dislike", "Не нравится"),
    REWRITTEN("rewritten", "Переписан разработчиком"),
    DELETED("deleted", "Удален разработчиком");

    private final String id;
    private final String label;

    FeedbackKind(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String id() {
        // Возвращает идентификатор вида отзыва
        return id;
    }

    public String label() {
        // Возвращает подпись вида отзыва
        return label;
    }

    public static Optional<FeedbackKind> fromId(String value) {
        // Находит вид отзыва по идентификатору
        return Arrays.stream(values()).filter(kind -> kind.id.equals(value)).findFirst();
    }
}
