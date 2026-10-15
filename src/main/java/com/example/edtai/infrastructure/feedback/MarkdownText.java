package com.example.edtai.infrastructure.feedback;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

final class MarkdownText {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");

    private MarkdownText() {
    }

    static String quote(String value) {
        // Экранирует строку для YAML-шапки
        String text = value == null ? "" : value;
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    static String list(List<String> values) {
        // Записывает список в формате YAML
        return values.stream().map(MarkdownText::quote).collect(Collectors.joining(", ", "[", "]"));
    }

    static String unquote(String value) {
        // Снимает кавычки со значения YAML
        String text = value.strip();
        if (text.length() >= 2 && (text.startsWith("\"") && text.endsWith("\"")
                || text.startsWith("'") && text.endsWith("'"))) {
            text = text.substring(1, text.length() - 1).replace("\\n", "\n").replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
        return text;
    }

    static String fence(String code, String path) {
        // Оформляет код в блок Markdown
        String marker = code.contains("```") ? "````" : "```";
        int dot = path.lastIndexOf('.');
        String language = dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.ROOT);
        return marker + ("os".equals(language) ? "bsl" : language) + "\n" + code.stripTrailing() + "\n" + marker;
    }

    static String now() {
        // Возвращает текущее время в ISO-8601
        return OffsetDateTime.now().withNano(0).toString();
    }

    static String fileName(String kind) {
        // Создает уникальное имя файла записи
        return OffsetDateTime.now().format(FILE_TIME) + "_" + kind + "_"
                + UUID.randomUUID().toString().substring(0, 8) + ".md";
    }
}
