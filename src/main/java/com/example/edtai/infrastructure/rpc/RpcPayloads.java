package com.example.edtai.infrastructure.rpc;

import com.example.edtai.domain.Feedback;
import com.example.edtai.domain.UiAnswer;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

final class RpcPayloads {

    private RpcPayloads() {
    }

    static JsonObject of(String... pairs) {
        // Собирает объект из пар ключ-значение
        JsonObject payload = new JsonObject();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            payload.addProperty(pairs[i], pairs[i + 1]);
        }
        return payload;
    }

    static JsonObject answer(UiAnswer answer) {
        // Собирает ответ на запрос расширения
        JsonObject record = of("type", "extension_ui_response", "id", answer.id());
        if (answer.cancelled()) {
            record.addProperty("cancelled", true);
        } else if (answer.confirmed() != null) {
            record.addProperty("confirmed", answer.confirmed());
        } else {
            record.addProperty("value", answer.value());
        }
        return record;
    }

    static JsonObject feedback(Feedback feedback) {
        // Собирает отзыв о коде для агента
        JsonObject data = new JsonObject();
        data.addProperty("kind", feedback.kind() == null ? "" : feedback.kind().id());
        data.addProperty("target", feedback.target());
        data.addProperty("file", feedback.path());
        data.addProperty("startLine", feedback.startLine());
        data.addProperty("endLine", feedback.endLine());
        JsonArray categories = new JsonArray();
        feedback.categories().forEach(categories::add);
        data.add("categories", categories);
        data.addProperty("comment", feedback.comment());
        data.addProperty("originalCode", feedback.originalCode());
        data.addProperty("rewrittenCode", feedback.currentCode());
        JsonObject payload = new JsonObject();
        payload.add("feedback", data);
        return payload;
    }
}
