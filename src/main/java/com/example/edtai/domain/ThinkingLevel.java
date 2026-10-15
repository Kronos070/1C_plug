package com.example.edtai.domain;

import java.util.Arrays;
import java.util.Optional;

public enum ThinkingLevel {
    OFF("off", "Без рассуждений"),
    MINIMAL("minimal", "Минимальное"),
    LOW("low", "Низкое"),
    MEDIUM("medium", "Среднее"),
    HIGH("high", "Высокое"),
    XHIGH("xhigh", "Очень высокое"),
    MAX("max", "Максимальное");

    private final String id;
    private final String label;

    ThinkingLevel(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String id() {
        // Возвращает идентификатор уровня
        return id;
    }

    public String label() {
        // Возвращает подпись уровня
        return label;
    }

    public static Optional<ThinkingLevel> fromId(String value) {
        // Ищет уровень по идентификатору
        return Arrays.stream(values()).filter(level -> level.id.equals(value)).findFirst();
    }
}
