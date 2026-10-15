package com.example.edtai.domain;

import java.util.Arrays;
import java.util.List;

public record TextShape(List<String> lines, String bom, String ending, boolean finalNewline) {

    public static TextShape of(String text) {
        // Разбирает текст на строки с учетом формата
        String value = text == null ? "" : text;
        String bom = value.startsWith("﻿") ? "﻿" : "";
        String body = value.substring(bom.length());
        String ending = body.contains("\r\n") ? "\r\n" : "\n";
        String normalized = body.replace("\r\n", "\n");
        boolean finalNewline = normalized.endsWith("\n");
        String trimmed = finalNewline ? normalized.substring(0, normalized.length() - 1) : normalized;
        List<String> lines = trimmed.isEmpty() && !finalNewline ? List.of() : Arrays.asList(trimmed.split("\n", -1));
        return new TextShape(List.copyOf(lines), bom, ending, finalNewline);
    }

    public String join(List<String> content) {
        // Собирает текст обратно в исходном формате
        String text = String.join(ending, content);
        return bom + text + (finalNewline && !content.isEmpty() ? ending : "");
    }
}
