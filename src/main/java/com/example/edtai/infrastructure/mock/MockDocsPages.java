package com.example.edtai.infrastructure.mock;

import com.example.edtai.infrastructure.rpc.JsonFields;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Locale;
import java.util.Map;

final class MockDocsPages {

    private static final int SNIPPET_RADIUS = 50;

    private final MockDocsData data;

    MockDocsPages(MockDocsData data) {
        this.data = data;
    }

    JsonObject page(String id) {
        // Строит страницу модуля или символа
        JsonObject module = data.module(id);
        if (module != null) {
            return modulePage(module);
        }
        JsonObject symbol = data.symbols().get(id);
        if (symbol == null) {
            throw new IllegalArgumentException("Документация не найдена: " + id);
        }
        JsonObject owner = data.owner(id);
        JsonObject page = symbol.deepCopy();
        page.remove("name");
        page.remove("reason");
        page.addProperty("id", id);
        page.addProperty("title", JsonFields.text(symbol, "name"));
        page.addProperty("path", JsonFields.text(owner, "id"));
        JsonArray crumbs = crumbs(owner);
        crumbs.add(JsonFields.text(symbol, "name"));
        page.add("breadcrumbs", crumbs);
        JsonObject link = new JsonObject();
        link.addProperty("id", JsonFields.text(owner, "id"));
        link.addProperty("title", JsonFields.text(owner, "title"));
        page.add("module", link);
        page.add("members", new JsonArray());
        page.addProperty("revision", data.revision());
        return page;
    }

    JsonArray search(String query, String kind, int limit) {
        // Ищет по именам, затем по текстам
        String needle = query.toLowerCase(Locale.ROOT);
        JsonArray byName = new JsonArray();
        JsonArray byText = new JsonArray();
        for (Map.Entry<String, JsonObject> entry : data.symbols().entrySet()) {
            JsonObject symbol = entry.getValue();
            if (!kind.isEmpty() && !kind.equals(JsonFields.text(symbol, "kind"))) {
                continue;
            }
            String text = JsonFields.text(symbol, "comment") + " " + noteText(symbol);
            if (JsonFields.text(symbol, "name").toLowerCase(Locale.ROOT).contains(needle)) {
                byName.add(hit(entry.getKey(), symbol, JsonFields.text(JsonFields.object(symbol, "note"), "summary")));
            } else if (text.toLowerCase(Locale.ROOT).contains(needle)) {
                byText.add(hit(entry.getKey(), symbol, snippet(text, needle)));
            }
        }
        byName.addAll(byText);
        JsonArray hits = new JsonArray();
        for (int i = 0; i < Math.min(limit, byName.size()); i++) {
            hits.add(byName.get(i));
        }
        return hits;
    }

    JsonArray outdated() {
        // Собирает символы, требующие внимания
        JsonArray items = new JsonArray();
        data.symbols().forEach((id, symbol) -> {
            String reason = MockDocsData.stale(symbol) ? "stale" : JsonFields.text(symbol, "reason");
            if (!reason.isEmpty()) {
                JsonObject item = hit(id, symbol, "");
                item.remove("snippet");
                item.remove("documented");
                item.addProperty("reason", reason);
                items.add(item);
            }
        });
        return items;
    }

    private JsonObject modulePage(JsonObject module) {
        // Строит страницу модуля с составом
        String id = JsonFields.text(module, "id");
        JsonObject page = new JsonObject();
        page.addProperty("id", id);
        page.addProperty("kind", "module");
        page.addProperty("title", JsonFields.text(module, "title"));
        page.addProperty("path", id);
        page.add("breadcrumbs", crumbs(module));
        page.addProperty("line", 1);
        page.add("parameters", new JsonArray());
        page.addProperty("comment", JsonFields.text(module, "comment"));
        page.add("calls", new JsonArray());
        if (module.has("note")) {
            page.add("note", module.get("note"));
        }
        JsonArray members = new JsonArray();
        for (JsonElement element : JsonFields.array(module, "symbols")) {
            JsonObject symbol = element.getAsJsonObject();
            JsonObject member = new JsonObject();
            member.addProperty("id", id + "#" + JsonFields.text(symbol, "name"));
            member.addProperty("kind", JsonFields.text(symbol, "kind"));
            member.addProperty("name", JsonFields.text(symbol, "name"));
            member.addProperty("line", JsonFields.number(symbol, "line"));
            member.addProperty("isExport", JsonFields.flag(symbol, "isExport"));
            member.addProperty("summary", JsonFields.text(JsonFields.object(symbol, "note"), "summary"));
            member.addProperty("documented", MockDocsData.documented(symbol));
            member.addProperty("stale", MockDocsData.stale(symbol));
            members.add(member);
        }
        page.add("members", members);
        page.addProperty("revision", data.revision());
        return page;
    }

    private JsonObject hit(String id, JsonObject symbol, String snippet) {
        // Строит результат поиска для символа
        JsonObject owner = data.owner(id);
        JsonObject hit = new JsonObject();
        hit.addProperty("id", id);
        hit.addProperty("kind", JsonFields.text(symbol, "kind"));
        hit.addProperty("title", JsonFields.text(symbol, "name"));
        hit.addProperty("moduleTitle", JsonFields.text(owner, "title"));
        hit.addProperty("path", JsonFields.text(owner, "id"));
        hit.addProperty("snippet", snippet);
        hit.addProperty("documented", MockDocsData.documented(symbol));
        return hit;
    }

    private static JsonArray crumbs(JsonObject module) {
        // Собирает подписи пути модуля
        JsonArray crumbs = new JsonArray();
        for (JsonElement folder : JsonFields.array(module, "folders")) {
            crumbs.add(JsonFields.text(folder.getAsJsonObject(), "label"));
        }
        crumbs.add(JsonFields.text(module, "label"));
        return crumbs;
    }

    private static String noteText(JsonObject symbol) {
        // Склеивает тексты описания символа
        JsonObject note = JsonFields.object(symbol, "note");
        StringBuilder text = new StringBuilder(JsonFields.text(note, "summary")).append(' ')
                .append(JsonFields.text(note, "details"));
        for (JsonElement item : JsonFields.array(note, "cases")) {
            text.append(' ').append(JsonFields.text(item.getAsJsonObject(), "text"));
        }
        return text.toString();
    }

    private static String snippet(String text, String needle) {
        // Вырезает фрагмент и отмечает совпадение
        String flat = text.replaceAll("\\s+", " ").strip();
        int at = flat.toLowerCase(Locale.ROOT).indexOf(needle);
        int start = Math.max(0, at - SNIPPET_RADIUS);
        int end = Math.min(flat.length(), at + needle.length() + SNIPPET_RADIUS);
        return (start > 0 ? "… " : "") + flat.substring(start, at) + "«" + flat.substring(at, at + needle.length())
                + "»" + flat.substring(at + needle.length(), end) + (end < flat.length() ? " …" : "");
    }
}
