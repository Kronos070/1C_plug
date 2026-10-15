package com.example.edtai.infrastructure.session;

import com.example.edtai.application.port.SessionCatalog;
import com.example.edtai.domain.SessionSummary;
import com.example.edtai.infrastructure.rpc.JsonFields;
import com.example.edtai.infrastructure.rpc.ToolArguments;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class FileSessionCatalog implements SessionCatalog {

    private static final int LIMIT = 100;
    private static final int TITLE_LIMIT = 90;

    @Override
    public List<SessionSummary> list(String currentSessionFile) {
        // Возвращает сессии из каталога текущей
        if (currentSessionFile == null || currentSessionFile.isBlank()) {
            return List.of();
        }
        Path directory = Path.of(currentSessionFile).getParent();
        if (directory == null || !Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(file -> file.getFileName().toString().endsWith(".jsonl"))
                    .map(FileSessionCatalog::summarize)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingLong(SessionSummary::modified).reversed())
                    .limit(LIMIT)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать историю сессий", e);
        }
    }

    private static SessionSummary summarize(Path file) {
        // Читает заголовок и первое сообщение
        String name = "";
        String firstMessage = "";
        int count = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("\"session_info\"")) {
                    String value = JsonFields.text(parse(line), "name");
                    name = value.isBlank() ? name : value;
                } else if (line.contains("\"type\":\"message\"")) {
                    count++;
                    if (firstMessage.isBlank() && line.contains("\"role\":\"user\"")) {
                        firstMessage = userText(JsonFields.object(parse(line), "message"));
                    }
                }
            }
            if (firstMessage.isBlank() && name.isBlank()) {
                return null;
            }
            String title = name.isBlank() ? shorten(firstMessage) : name;
            return new SessionSummary(file.toString(), title, Files.getLastModifiedTime(file).toMillis(), count);
        } catch (IOException | UncheckedIOException e) {
            return null;
        }
    }

    private static JsonObject parse(String line) {
        // Разбирает строку журнала сессии
        try {
            JsonElement element = JsonParser.parseString(line);
            return element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        } catch (JsonParseException e) {
            return new JsonObject();
        }
    }

    private static String userText(JsonObject message) {
        // Извлекает текст сообщения пользователя
        JsonElement content = message.get("content");
        if (content != null && content.isJsonArray()) {
            return ToolArguments.joinText(content.getAsJsonArray());
        }
        return JsonFields.text(message, "content");
    }

    private static String shorten(String text) {
        // Сокращает текст до заголовка
        String single = text.replaceAll("\\s+", " ").strip();
        return single.length() <= TITLE_LIMIT ? single : single.substring(0, TITLE_LIMIT) + "…";
    }
}
