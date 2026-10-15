package com.example.edtai.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum AgentMode {
    FIND("find", "Найти", "НАЙТИ", "Поиск по конфигурации без изменений"),
    EXPLAIN("explain", "Объяснить", "ОБЪЯСНИТЬ", "Разбор кода и документация"),
    WRITE("write", "Написать", "НАПИСАТЬ", "Изменение кода и метаданных"),
    PLAN("plan", "Планировать", "ПЛАНИРОВАТЬ", "Проектирование без изменений");

    private final String id;
    private final String label;
    private final String marker;
    private final String hint;

    AgentMode(String id, String label, String marker, String hint) {
        this.id = id;
        this.label = label;
        this.marker = marker;
        this.hint = hint;
    }

    public String id() {
        // Возвращает идентификатор режима
        return id;
    }

    public String label() {
        // Возвращает подпись режима
        return label;
    }

    public String hint() {
        // Возвращает описание режима
        return hint;
    }

    public static Optional<AgentMode> fromId(String value) {
        // Ищет режим по идентификатору
        return Arrays.stream(values()).filter(mode -> mode.id.equals(value)).findFirst();
    }

    public static Optional<AgentMode> fromStatus(String status) {
        // Распознает режим из статуса
        String upper = status == null ? "" : status.toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(mode -> upper.contains(mode.marker)).findFirst();
    }
}
