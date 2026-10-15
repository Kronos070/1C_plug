package com.example.edtai.domain;

public record ReviewBaseline(Kind kind, String text) {

    public enum Kind {
        TEXT,
        ABSENT,
        UNKNOWN
    }

    public ReviewBaseline {
        text = text == null ? "" : text;
    }

    public static ReviewBaseline of(String text) {
        // Создает исходник с известным текстом
        return new ReviewBaseline(Kind.TEXT, text);
    }

    public static ReviewBaseline absent() {
        // Создает исходник для нового файла
        return new ReviewBaseline(Kind.ABSENT, "");
    }

    public static ReviewBaseline unknown() {
        // Создает исходник без сохраненного текста
        return new ReviewBaseline(Kind.UNKNOWN, "");
    }
}
