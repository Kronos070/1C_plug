package com.example.edtai.domain;

public enum ChangeKind {
    ADDED("added"),
    MODIFIED("modified"),
    DELETED("deleted");

    private final String id;

    ChangeKind(String id) {
        this.id = id;
    }

    public String id() {
        // Возвращает идентификатор вида изменения
        return id;
    }
}
