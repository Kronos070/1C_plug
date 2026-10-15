package com.example.edtai.domain;

import java.util.Arrays;
import java.util.Optional;

public enum DocSection {
    MODULE("module"),
    PROCEDURE("procedure"),
    FUNCTION("function"),
    VARIABLE("variable");

    private final String id;

    DocSection(String id) {
        this.id = id;
    }

    public String id() {
        // Возвращает идентификатор раздела
        return id;
    }

    public static Optional<DocSection> fromId(String value) {
        // Находит раздел по идентификатору
        return Arrays.stream(values()).filter(section -> section.id.equals(value)).findFirst();
    }
}
