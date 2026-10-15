package com.example.edtai.domain;

public record DocCase(String title, String text) {

    public DocCase {
        title = title == null ? "" : title.strip();
        text = text == null ? "" : text.strip();
    }

    public boolean isBlank() {
        // Проверяет, что случай пустой
        return title.isEmpty() && text.isEmpty();
    }
}
