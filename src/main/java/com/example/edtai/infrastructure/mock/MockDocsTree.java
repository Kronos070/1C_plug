package com.example.edtai.infrastructure.mock;

import com.example.edtai.infrastructure.rpc.JsonFields;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MockDocsTree {

    private final MockDocsData data;

    MockDocsTree(MockDocsData data) {
        this.data = data;
    }

    JsonArray children(String section, String node) {
        // Строит прямых потомков узла раздела
        JsonArray items = new JsonArray();
        JsonObject module = data.module(node);
        if (module != null && !"module".equals(section)) {
            items(module, section).forEach(symbol -> items.add(symbolNode(module, symbol)));
            return items;
        }
        Map<String, List<JsonObject>> groups = new LinkedHashMap<>();
        for (JsonObject candidate : data.modules()) {
            if (items(candidate, section).isEmpty()) {
                continue;
            }
            String child = childOf(candidate, node);
            if (child != null) {
                groups.computeIfAbsent(child, key -> new ArrayList<>()).add(candidate);
            }
        }
        groups.forEach((id, members) -> items.add(groupNode(id, members, section)));
        return items;
    }

    List<JsonObject> items(JsonObject module, String section) {
        // Отбирает символы модуля для раздела
        List<JsonObject> result = new ArrayList<>();
        for (JsonElement element : JsonFields.array(module, "symbols")) {
            JsonObject symbol = element.getAsJsonObject();
            if ("module".equals(section) || section.equals(JsonFields.text(symbol, "kind"))) {
                result.add(symbol);
            }
        }
        return result;
    }

    private String childOf(JsonObject module, String node) {
        // Находит следующий уровень пути модуля
        JsonArray folders = JsonFields.array(module, "folders");
        if (node.isEmpty()) {
            return folders.size() == 0 ? JsonFields.text(module, "id") : id(folders.get(0));
        }
        for (int i = 0; i < folders.size(); i++) {
            if (id(folders.get(i)).equals(node)) {
                return i + 1 < folders.size() ? id(folders.get(i + 1)) : JsonFields.text(module, "id");
            }
        }
        return null;
    }

    private JsonObject groupNode(String id, List<JsonObject> modules, String section) {
        // Строит узел папки или модуля
        JsonObject module = data.module(id);
        int count = 0;
        boolean documented = true;
        boolean stale = false;
        for (JsonObject item : modules) {
            for (JsonObject symbol : items(item, section)) {
                count++;
                documented &= MockDocsData.documented(symbol);
                stale |= MockDocsData.stale(symbol);
            }
        }
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("label", module != null ? JsonFields.text(module, "label") : folderLabel(modules.get(0), id));
        json.addProperty("type", module != null ? "module" : "folder");
        if (module != null) {
            json.addProperty("kind", "module");
            json.addProperty("docId", id);
        }
        json.addProperty("count", "module".equals(section) && module == null ? modules.size() : count);
        json.addProperty("hasChildren", module == null || !"module".equals(section));
        json.addProperty("documented", documented);
        json.addProperty("stale", stale);
        return json;
    }

    private JsonObject symbolNode(JsonObject module, JsonObject symbol) {
        // Строит узел символа модуля
        String id = JsonFields.text(module, "id") + "#" + JsonFields.text(symbol, "name");
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("label", JsonFields.text(symbol, "name"));
        json.addProperty("type", "symbol");
        json.addProperty("kind", JsonFields.text(symbol, "kind"));
        json.addProperty("docId", id);
        json.addProperty("count", 0);
        json.addProperty("hasChildren", false);
        json.addProperty("isExport", JsonFields.flag(symbol, "isExport"));
        json.addProperty("documented", MockDocsData.documented(symbol));
        json.addProperty("stale", MockDocsData.stale(symbol));
        return json;
    }

    private static String folderLabel(JsonObject module, String id) {
        // Находит подпись папки по идентификатору
        for (JsonElement folder : JsonFields.array(module, "folders")) {
            if (id(folder).equals(id)) {
                return JsonFields.text(folder.getAsJsonObject(), "label");
            }
        }
        return id;
    }

    private static String id(JsonElement folder) {
        // Возвращает идентификатор папки
        return JsonFields.text(folder.getAsJsonObject(), "id");
    }
}
