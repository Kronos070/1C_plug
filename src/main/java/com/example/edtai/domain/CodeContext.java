package com.example.edtai.domain;

import java.util.List;
import java.util.Objects;

public record CodeContext(String path, int startLine, int endLine, String code, List<CodeProblem> problems) {

    public CodeContext {
        path = Objects.requireNonNull(path);
        code = code == null ? "" : code;
        problems = problems == null ? List.of() : List.copyOf(problems);
    }

    public static CodeContext file(String path) {
        // Создает контекст всего файла
        return new CodeContext(path, 0, 0, "", List.of());
    }

    public static CodeContext selection(String path, int startLine, int endLine, String code) {
        // Создает контекст выделенного фрагмента
        return new CodeContext(path, startLine, endLine, code, List.of());
    }

    public CodeContext withProblems(List<CodeProblem> list) {
        // Возвращает контекст с замечаниями EDT
        return new CodeContext(path, startLine, endLine, code, list);
    }

    public boolean hasCode() {
        // Проверяет наличие выделенного кода
        return !code.isEmpty();
    }

    public String id() {
        // Вычисляет идентификатор контекста
        return Integer.toHexString(Objects.hash(path, startLine, endLine, code));
    }
}
