package com.example.edtai.ui.docs;

import com.example.edtai.domain.DocCaller;
import com.example.edtai.domain.DocCase;
import com.example.edtai.domain.DocNoteDraft;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

final class DocsMessages {

    private DocsMessages() {
    }

    static JsonObject loaded(String kind, String key, JsonObject data) {
        // Строит событие с ответом документации
        JsonObject json = typed("docs");
        json.addProperty("kind", kind);
        json.addProperty("key", key);
        json.add("data", data);
        return json;
    }

    static JsonObject failed(String kind, String key, String text) {
        // Строит событие об ошибке запроса
        JsonObject json = typed("docsError");
        json.addProperty("kind", kind);
        json.addProperty("key", key);
        json.addProperty("text", text);
        return json;
    }

    static JsonObject availability(boolean supported) {
        // Строит событие о поддержке документации
        JsonObject json = typed("docsAvailability");
        json.addProperty("supported", supported);
        return json;
    }

    static JsonObject signal(String text) {
        // Строит событие о статусе индексации
        JsonObject json = typed("docsSignal");
        json.addProperty("text", text);
        return json;
    }

    static JsonObject open(String id) {
        // Строит событие открытия записи
        JsonObject json = typed("docsOpen");
        json.addProperty("id", id);
        return json;
    }

    static JsonObject notice(String level, String text) {
        // Строит уведомление для страницы
        JsonObject json = typed("notice");
        json.addProperty("level", level);
        json.addProperty("text", text);
        return json;
    }

    static JsonObject callers(List<DocCaller> callers) {
        // Переводит вызывающие методы в JSON
        JsonArray items = new JsonArray();
        for (DocCaller caller : callers) {
            JsonObject item = new JsonObject();
            item.addProperty("id", caller.id());
            item.addProperty("name", caller.name());
            item.addProperty("path", caller.path());
            item.addProperty("line", caller.line());
            items.add(item);
        }
        JsonObject data = new JsonObject();
        data.add("items", items);
        return data;
    }

    static boolean flag(JsonObject command, String key) {
        // Читает логическое поле команды
        return command.has(key) && command.get(key).isJsonPrimitive() && command.get(key).getAsBoolean();
    }

    static DocNoteDraft draft(JsonObject command) {
        // Собирает черновик описания из команды
        List<DocCase> cases = new ArrayList<>();
        JsonElement items = command.get("cases");
        if (items != null && items.isJsonArray()) {
            for (JsonElement item : items.getAsJsonArray()) {
                if (item.isJsonObject()) {
                    cases.add(new DocCase(text(item.getAsJsonObject(), "title"), text(item.getAsJsonObject(), "text")));
                }
            }
        }
        return new DocNoteDraft(text(command, "summary"), text(command, "details"), cases);
    }

    static String text(JsonObject command, String key) {
        // Читает строковое поле команды
        return command.has(key) && command.get(key).isJsonPrimitive() ? command.get(key).getAsString() : "";
    }

    static int number(JsonObject command, String key) {
        // Читает числовое поле команды
        return command.has(key) && command.get(key).isJsonPrimitive() ? command.get(key).getAsInt() : 0;
    }

    private static JsonObject typed(String type) {
        // Создает JSON с типом события
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        return json;
    }
}
