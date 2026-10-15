package com.example.edtai.infrastructure.mock;

import com.example.edtai.infrastructure.rpc.JsonFields;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MockDocsData {

    private static final String RESOURCE = "/mock/docs-demo.json";

    private final JsonObject root;
    private final List<JsonObject> modules = new ArrayList<>();
    private final Map<String, JsonObject> symbols = new LinkedHashMap<>();
    private final Map<String, JsonObject> owners = new LinkedHashMap<>();

    MockDocsData() {
        root = read();
        for (JsonElement element : JsonFields.array(root, "modules")) {
            JsonObject module = element.getAsJsonObject();
            modules.add(module);
            for (JsonElement item : JsonFields.array(module, "symbols")) {
                JsonObject symbol = item.getAsJsonObject();
                String id = JsonFields.text(module, "id") + "#" + JsonFields.text(symbol, "name");
                symbols.put(id, symbol);
                owners.put(id, module);
            }
        }
    }

    String revision() {
        // Возвращает коммит демо-индекса
        return JsonFields.text(root, "revision");
    }

    String syncedAt() {
        // Возвращает время демо-индексации
        return JsonFields.text(root, "syncedAt");
    }

    List<JsonObject> modules() {
        // Возвращает модули в порядке файла
        return modules;
    }

    Map<String, JsonObject> symbols() {
        // Возвращает символы по идентификатору
        return symbols;
    }

    JsonObject module(String id) {
        // Находит модуль по идентификатору
        return modules.stream().filter(item -> JsonFields.text(item, "id").equals(id)).findFirst().orElse(null);
    }

    JsonObject owner(String symbolId) {
        // Возвращает модуль символа
        return owners.get(symbolId);
    }

    static boolean documented(JsonObject entry) {
        // Проверяет наличие описания
        return entry.has("note");
    }

    static boolean stale(JsonObject entry) {
        // Проверяет, устарело ли описание
        return JsonFields.flag(JsonFields.object(entry, "note"), "stale");
    }

    private static JsonObject read() {
        // Читает демо-документацию из ресурсов
        try (InputStream in = MockDocsData.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Не найден ресурс " + RESOURCE);
            }
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
