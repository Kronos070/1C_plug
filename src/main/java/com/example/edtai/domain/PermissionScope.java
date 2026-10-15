package com.example.edtai.domain;

import java.util.Arrays;

public enum PermissionScope {
    ONCE("once"),
    SESSION("session"),
    ALWAYS("always"),
    NEVER("never");

    private final String id;

    PermissionScope(String id) {
        this.id = id;
    }

    public static PermissionScope fromId(String value) {
        // Находит область решения по идентификатору
        return Arrays.stream(values()).filter(scope -> scope.id.equals(value)).findFirst().orElse(ONCE);
    }
}
