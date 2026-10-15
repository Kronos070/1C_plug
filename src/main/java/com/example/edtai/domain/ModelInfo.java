package com.example.edtai.domain;

public record ModelInfo(String provider, String id, String name, boolean reasoning) {

    public String key() {
        // Возвращает уникальный ключ модели
        return provider + "/" + id;
    }

    public boolean isKnown() {
        // Проверяет, что модель выбрана
        return !id.isBlank() && !"unknown".equals(id);
    }
}
