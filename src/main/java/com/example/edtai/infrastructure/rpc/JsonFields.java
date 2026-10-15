package com.example.edtai.infrastructure.rpc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class JsonFields {

    private JsonFields() {
    }

    public static String text(JsonObject object, String key) {
        // Читает строковое поле безопасно
        JsonElement value = object == null ? null : object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
    }

    public static boolean flag(JsonObject object, String key) {
        // Читает логическое поле безопасно
        JsonElement value = object == null ? null : object.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsBoolean();
    }

    public static int number(JsonObject object, String key) {
        // Читает целое поле безопасно
        JsonElement value = object == null ? null : object.get(key);
        return isNumber(value) ? value.getAsInt() : 0;
    }

    public static long timestamp(JsonObject object, String key) {
        // Читает отметку времени безопасно
        JsonElement value = object == null ? null : object.get(key);
        return isNumber(value) ? value.getAsLong() : 0L;
    }

    public static JsonObject object(JsonObject object, String key) {
        // Читает вложенный объект безопасно
        JsonElement value = object == null ? null : object.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject();
    }

    private static boolean isNumber(JsonElement value) {
        // Проверяет, что элемент является числом
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber();
    }

    public static JsonArray array(JsonObject object, String key) {
        // Читает массив безопасно
        JsonElement value = object == null ? null : object.get(key);
        return value != null && value.isJsonArray() ? value.getAsJsonArray() : new JsonArray();
    }
}
