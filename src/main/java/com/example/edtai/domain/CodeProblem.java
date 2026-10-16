package com.example.edtai.domain;

public record CodeProblem(Severity severity, int line, String message) {

    public enum Severity {
        ERROR("Ошибка"),
        WARNING("Предупреждение"),
        INFO("Замечание");

        private final String label;

        Severity(String label) {
            this.label = label;
        }

        public String label() {
            // Возвращает подпись важности
            return label;
        }
    }
}
