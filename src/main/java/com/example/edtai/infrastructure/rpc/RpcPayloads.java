package com.example.edtai.infrastructure.rpc;

import com.example.edtai.domain.UiAnswer;
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
}
