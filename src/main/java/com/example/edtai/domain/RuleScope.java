package com.example.edtai.domain;

import java.util.Arrays;

public enum RuleScope {
    MODULE("module", "Этот модуль"),
    FOLDER("folder", "Эта папка"),
    CONFIG("config", "Вся конфигурация");

    private final String id;
    private final String label;

    RuleScope(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String id() {
        // Возвращает идентификатор области
        return id;
    }

    public String label() {
        // Возвращает подпись области
        return label;
    }

    public static RuleScope fromId(String value) {
        // Находит область по идентификатору
        return Arrays.stream(values()).filter(scope -> scope.id.equals(value)).findFirst().orElse(CONFIG);
    }
}
