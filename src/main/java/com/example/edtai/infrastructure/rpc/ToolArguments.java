package com.example.edtai.infrastructure.rpc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;

public final class ToolArguments {

    private static final int SUMMARY_LIMIT = 160;
    private static final int OUTPUT_LIMIT = 6000;
    private static final List<String> KEYS = List.of("path", "file_path", "filePath", "command", "pattern",
            "query", "metadataName", "procedureName", "featurePath", "role", "action", "url");

    private ToolArguments() {
    }

    public static String summarize(JsonObject arguments) {
        // Кратко описывает аргументы инструмента
        if (arguments == null || arguments.size() == 0) {
            return "";
        }
        for (String key : KEYS) {
            String value = JsonFields.text(arguments, key);
            if (!value.isBlank()) {
                return limit(value, SUMMARY_LIMIT);
            }
        }
        return limit(arguments.toString(), SUMMARY_LIMIT);
    }

    public static String output(JsonObject result) {
        // Извлекает текст результата инструмента
        return limit(joinText(JsonFields.array(result, "content")), OUTPUT_LIMIT);
    }

    public static String joinText(JsonArray content) {
        // Склеивает текстовые блоки содержимого
        StringBuilder text = new StringBuilder();
        for (JsonElement element : content) {
            if (element.isJsonObject() && "text".equals(JsonFields.text(element.getAsJsonObject(), "type"))) {
                if (text.length() > 0) {
                    text.append('\n');
                }
                text.append(JsonFields.text(element.getAsJsonObject(), "text"));
            }
        }
        return text.toString();
    }

    private static String limit(String value, int max) {
        // Обрезает строку до лимита
        String single = value.strip();
        return single.length() <= max ? single : single.substring(0, max) + "…";
    }
}
