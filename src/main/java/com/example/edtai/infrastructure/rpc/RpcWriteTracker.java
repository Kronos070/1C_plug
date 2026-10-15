package com.example.edtai.infrastructure.rpc;

import com.example.edtai.domain.ChatEvent;
import com.example.edtai.domain.LineRange;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class RpcWriteTracker {

    private static final Set<String> WRITING_TOOLS = Set.of("edit", "write");

    private final Map<String, JsonObject> pending = new ConcurrentHashMap<>();

    Optional<ChatEvent> track(JsonObject record, Path workDirectory) {
        // Отслеживает успешные правки файлов агентом
        String type = JsonFields.text(record, "type");
        String id = JsonFields.text(record, "toolCallId");
        if ("tool_execution_start".equals(type) && WRITING_TOOLS.contains(JsonFields.text(record, "toolName"))) {
            JsonObject args = JsonFields.object(record, "args");
            args.addProperty("__tool", JsonFields.text(record, "toolName"));
            pending.put(id, args);
            return Optional.empty();
        }
        if (!"tool_execution_end".equals(type)) {
            return Optional.empty();
        }
        JsonObject args = pending.remove(id);
        if (args == null || JsonFields.flag(record, "isError") || JsonFields.text(args, "path").isBlank()) {
            return Optional.empty();
        }
        String path = workDirectory.resolve(JsonFields.text(args, "path")).normalize().toString();
        List<LineRange> ranges = parseRanges(record);
        return Optional.of(new ChatEvent.CodeWritten(path, snippets(args), ranges));
    }

    private static List<LineRange> parseRanges(JsonObject record) {
        // Извлекает диапазоны строк из результата инструмента
        if (!record.has("result") || !record.get("result").isJsonObject()) {
            return List.of();
        }
        JsonObject result = record.getAsJsonObject("result");
        if (!result.has("details") || !result.get("details").isJsonObject()) {
            return List.of();
        }
        JsonObject details = result.getAsJsonObject("details");
        if (!details.has("ranges") || !details.get("ranges").isJsonArray()) {
            return List.of();
        }
        JsonArray array = details.getAsJsonArray("ranges");
        List<LineRange> ranges = new ArrayList<>();
        for (JsonElement element : array) {
            if (element.isJsonObject()) {
                JsonObject range = element.getAsJsonObject();
                int start = JsonFields.number(range, "startLine");
                int end = JsonFields.number(range, "endLine");
                if (start > 0 && end >= start) {
                    ranges.add(new LineRange(start, end));
                }
            }
        }
        return List.copyOf(ranges);
    }

    private static List<String> snippets(JsonObject args) {
        // Извлекает записанные фрагменты кода
        List<String> result = new ArrayList<>();
        if ("write".equals(JsonFields.text(args, "__tool"))) {
            result.add(JsonFields.text(args, "content"));
            return result;
        }
        for (JsonElement edit : JsonFields.array(args, "edits")) {
            if (edit.isJsonObject()) {
                JsonObject change = edit.getAsJsonObject();
                result.add(changedPart(JsonFields.text(change, "oldText"), JsonFields.text(change, "newText")));
            }
        }
        if (result.isEmpty()) {
            result.add(changedPart(JsonFields.text(args, "oldText"), JsonFields.text(args, "newText")));
        }
        return result;
    }

    private static String changedPart(String before, String after) {
        // Оставляет только измененные строки правки
        List<String> old = before.lines().toList();
        List<String> next = after.lines().toList();
        int prefix = 0;
        while (prefix < old.size() && prefix < next.size() && old.get(prefix).equals(next.get(prefix))) {
            prefix++;
        }
        int suffix = 0;
        while (suffix < old.size() - prefix && suffix < next.size() - prefix
                && old.get(old.size() - 1 - suffix).equals(next.get(next.size() - 1 - suffix))) {
            suffix++;
        }
        return String.join("\n", next.subList(prefix, next.size() - suffix));
    }
}
