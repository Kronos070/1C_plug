package com.example.edtai.ui.bridge;

import com.example.edtai.application.DocsService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

final class DocsPeek {

    private static final int LIMIT = 12;

    private DocsPeek() {
    }

    static void peek(DocsService service, JsonElement ids, String key, Consumer<JsonObject> post) {
        // Подгружает краткие сведения о записях
        if (!service.available() || ids == null || !ids.isJsonArray()) {
            return;
        }
        List<CompletableFuture<JsonObject>> pages = new ArrayList<>();
        for (JsonElement id : ids.getAsJsonArray()) {
            if (pages.size() < LIMIT && id.isJsonPrimitive()) {
                pages.add(service.page(id.getAsString()).thenApply(DocsPeek::brief)
                        .exceptionally(error -> missing(id.getAsString())));
            }
        }
        CompletableFuture.allOf(pages.toArray(CompletableFuture[]::new)).thenRun(() -> {
            JsonArray items = new JsonArray();
            pages.forEach(page -> items.add(page.join()));
            JsonObject message = new JsonObject();
            message.addProperty("type", "docsPeek");
            message.addProperty("key", key);
            message.add("items", items);
            post.accept(message);
        });
    }

    private static JsonObject brief(JsonObject data) {
        // Оставляет только нужные плашке поля
        JsonObject page = data.getAsJsonObject("page");
        JsonObject brief = new JsonObject();
        brief.add("id", page.get("id"));
        brief.add("title", page.get("title"));
        brief.add("kind", page.get("kind"));
        if (page.has("note")) {
            JsonObject note = page.getAsJsonObject("note");
            brief.add("author", note.get("author"));
            brief.add("updatedAt", note.get("updatedAt"));
            brief.add("stale", note.get("stale"));
        }
        return brief;
    }

    private static JsonObject missing(String id) {
        // Помечает запись, которой нет в индексе
        JsonObject brief = new JsonObject();
        brief.addProperty("id", id);
        brief.addProperty("missing", true);
        return brief;
    }
}
