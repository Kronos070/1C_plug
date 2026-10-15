package com.example.edtai.infrastructure.mock;

import com.example.edtai.application.port.DocsGateway;
import com.example.edtai.infrastructure.rpc.JsonFields;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public final class MockDocsGateway implements DocsGateway {

    private static final MockDocsGateway SHARED = new MockDocsGateway();
    private static final int DEFAULT_LIMIT = 30;

    private volatile Path root = Path.of("");
    private MockDocsData data;
    private MockDocsTree tree;
    private MockDocsPages pages;

    private MockDocsGateway() {
    }

    public static MockDocsGateway shared() {
        // Возвращает общий демо-шлюз документации
        return SHARED;
    }

    @Override
    public boolean supported() {
        // Демо-режим всегда поддерживает документацию
        return true;
    }

    @Override
    public Path workDirectory() {
        // Возвращает каталог демо-проекта
        return root;
    }

    public void root(Path value) {
        // Запоминает каталог демо-проекта
        root = value;
    }

    @Override
    public synchronized CompletableFuture<JsonObject> call(String method, JsonObject params) {
        // Выполняет метод документации на демо-данных
        try {
            return CompletableFuture.completedFuture(dispatch(method, params == null ? new JsonObject() : params));
        } catch (IllegalArgumentException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private JsonObject dispatch(String method, JsonObject params) {
        // Направляет вызов к нужному методу
        load();
        String id = JsonFields.text(params, "id");
        JsonObject result = new JsonObject();
        switch (method) {
            case "docs.status" -> result.add("status", status());
            case "docs.sync" -> {
                result.add("report", report());
                result.add("status", status());
            }
            case "docs.tree" -> {
                result.addProperty("section", JsonFields.text(params, "section"));
                result.addProperty("node", JsonFields.text(params, "node"));
                result.add("items", tree.children(JsonFields.text(params, "section"), JsonFields.text(params, "node")));
                result.addProperty("syncing", false);
            }
            case "docs.page" -> result.add("page", pages.page(id));
            case "docs.search" -> {
                int limit = JsonFields.number(params, "limit");
                result.addProperty("query", JsonFields.text(params, "query"));
                result.add("hits", pages.search(JsonFields.text(params, "query"), JsonFields.text(params, "kind"),
                        limit <= 0 ? DEFAULT_LIMIT : limit));
            }
            case "docs.outdated" -> result.add("items", pages.outdated());
            case "docs.save" -> result.add("page", save(id, params));
            case "docs.remove" -> result.add("page", remove(id));
            default -> throw new IllegalArgumentException("Неизвестный метод документации: " + method);
        }
        return result;
    }

    private JsonObject save(String id, JsonObject params) {
        // Заменяет описание записи целиком
        JsonObject entry = entry(id);
        JsonObject note = new JsonObject();
        note.addProperty("summary", JsonFields.text(params, "summary"));
        note.addProperty("details", JsonFields.text(params, "details"));
        note.add("cases", JsonFields.array(params, "cases"));
        note.addProperty("author", "user");
        note.addProperty("updatedAt", Instant.now().toString());
        note.addProperty("stale", false);
        entry.add("note", note);
        entry.remove("reason");
        return pages.page(id);
    }

    private JsonObject remove(String id) {
        // Удаляет описание записи
        entry(id).remove("note");
        return pages.page(id);
    }

    private JsonObject entry(String id) {
        // Находит модуль или символ для правки
        JsonObject entry = data.module(id) != null ? data.module(id) : data.symbols().get(id);
        if (entry == null) {
            throw new IllegalArgumentException("Документация не найдена: " + id);
        }
        return entry;
    }

    private JsonObject status() {
        // Собирает состояние демо-индекса
        long documented = data.symbols().values().stream().filter(MockDocsData::documented).count();
        long stale = data.symbols().values().stream().filter(MockDocsData::stale).count();
        JsonObject status = new JsonObject();
        status.addProperty("root", "демо-конфигурация");
        status.addProperty("source", "git");
        status.addProperty("revision", data.revision());
        status.addProperty("syncedAt", data.syncedAt());
        status.addProperty("syncing", false);
        status.addProperty("modules", data.modules().size());
        status.addProperty("symbols", data.symbols().size());
        status.addProperty("documented", documented);
        status.addProperty("stale", stale);
        status.addProperty("outdated", pages.outdated().size());
        return status;
    }

    private JsonObject report() {
        // Собирает отчет демо-синхронизации
        JsonObject report = new JsonObject();
        report.addProperty("revision", data.revision());
        report.addProperty("added", 0);
        report.addProperty("changed", 0);
        report.addProperty("removed", 0);
        report.addProperty("upToDate", true);
        report.addProperty("durationMs", 12);
        return report;
    }

    private void load() {
        // Лениво загружает демо-данные
        if (data == null) {
            data = new MockDocsData();
            tree = new MockDocsTree(data);
            pages = new MockDocsPages(data);
        }
    }
}
