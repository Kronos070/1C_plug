package com.example.edtai.application;

import com.example.edtai.application.port.AgentUnavailableException;
import com.example.edtai.application.port.DocsGateway;
import com.example.edtai.domain.DocCase;
import com.example.edtai.domain.DocNoteDraft;
import com.example.edtai.domain.DocSection;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class DocsService {

    private static final int SEARCH_LIMIT = 30;

    private final Supplier<DocsGateway> gateway;

    public DocsService(Supplier<DocsGateway> gateway) {
        this.gateway = gateway;
    }

    public boolean available() {
        // Проверяет, отдает ли агент документацию
        DocsGateway current = gateway.get();
        return current != null && current.supported();
    }

    public CompletableFuture<JsonObject> status() {
        // Запрашивает состояние индекса
        return call("docs.status", new JsonObject());
    }

    public CompletableFuture<JsonObject> sync() {
        // Запускает переиндексацию по git
        return call("docs.sync", new JsonObject());
    }

    public CompletableFuture<JsonObject> tree(DocSection section, String node) {
        // Запрашивает потомков узла дерева
        JsonObject params = new JsonObject();
        params.addProperty("section", section.id());
        params.addProperty("node", node == null ? "" : node);
        return call("docs.tree", params);
    }

    public CompletableFuture<JsonObject> page(String id) {
        // Запрашивает страницу модуля или символа
        return call("docs.page", idParams(id));
    }

    public CompletableFuture<JsonObject> search(String query, Optional<DocSection> kind) {
        // Ищет по именам и описаниям
        JsonObject params = new JsonObject();
        params.addProperty("query", query.strip());
        params.addProperty("limit", SEARCH_LIMIT);
        kind.ifPresent(section -> params.addProperty("kind", section.id()));
        return call("docs.search", params);
    }

    public CompletableFuture<JsonObject> outdated() {
        // Запрашивает записи, требующие внимания
        return call("docs.outdated", new JsonObject());
    }

    public CompletableFuture<JsonObject> save(String id, DocNoteDraft draft) {
        // Сохраняет описание целиком
        if (draft.summary().isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Заполните поле «Кратко»"));
        }
        JsonObject params = idParams(id);
        params.addProperty("summary", draft.summary());
        params.addProperty("details", draft.details());
        JsonArray cases = new JsonArray();
        for (DocCase item : draft.cases()) {
            JsonObject json = new JsonObject();
            json.addProperty("title", item.title());
            json.addProperty("text", item.text());
            cases.add(json);
        }
        params.add("cases", cases);
        return call("docs.save", params);
    }

    public CompletableFuture<JsonObject> remove(String id) {
        // Удаляет описание записи
        return call("docs.remove", idParams(id));
    }

    private CompletableFuture<JsonObject> call(String method, JsonObject params) {
        // Вызывает метод документации агента
        DocsGateway current = gateway.get();
        if (current == null) {
            return CompletableFuture.failedFuture(new AgentUnavailableException("ПерУН не запущен"));
        }
        if (!current.supported()) {
            return CompletableFuture.failedFuture(
                    new AgentUnavailableException("Эта версия ПерУН не поддерживает документацию"));
        }
        return current.call(method, params);
    }

    private static JsonObject idParams(String id) {
        // Собирает параметры с идентификатором
        JsonObject params = new JsonObject();
        params.addProperty("id", id);
        return params;
    }
}
