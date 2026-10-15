package com.example.edtai.domain;

public record UiAnswer(String id, String value, Boolean confirmed, boolean cancelled) {

    public static UiAnswer confirm(String id, boolean confirmed) {
        // Создает ответ на подтверждение
        return new UiAnswer(id, null, confirmed, false);
    }

    public static UiAnswer value(String id, String value) {
        // Создает ответ со значением
        return new UiAnswer(id, value, null, false);
    }

    public static UiAnswer cancel(String id) {
        // Создает ответ об отмене
        return new UiAnswer(id, null, null, true);
    }
}
