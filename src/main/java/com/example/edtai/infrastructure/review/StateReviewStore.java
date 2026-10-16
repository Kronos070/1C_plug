package com.example.edtai.infrastructure.review;

import com.example.edtai.application.port.ReviewStore;
import com.example.edtai.domain.ReviewBaseline;
import com.example.edtai.domain.ReviewEntry;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class StateReviewStore implements ReviewStore {

    private static final String INDEX = "index.json";

    private final Supplier<Path> directory;
    private Map<String, ReviewEntry> entries;

    public StateReviewStore(Supplier<Path> directory) {
        this.directory = directory;
    }

    @Override
    public synchronized Optional<ReviewEntry> find(String path) {
        // Находит непринятые правки файла
        return Optional.ofNullable(entries().get(path));
    }

    @Override
    public synchronized List<ReviewEntry> all() {
        // Возвращает все файлы с правками
        return new ArrayList<>(entries().values());
    }

    @Override
    public synchronized void save(ReviewEntry entry) {
        // Сохраняет исходник и текст агента
        entries().put(entry.path(), entry);
        try {
            Files.createDirectories(directory.get());
            Files.writeString(file(entry.path(), "base"), entry.baseline().text(), StandardCharsets.UTF_8);
            Files.writeString(file(entry.path(), "ai"), entry.aiText(), StandardCharsets.UTF_8);
            writeIndex();
        } catch (IOException ignored) {
            return;
        }
    }

    @Override
    public synchronized void remove(String path) {
        // Забывает правки файла
        entries().remove(path);
        try {
            Files.deleteIfExists(file(path, "base"));
            Files.deleteIfExists(file(path, "ai"));
            writeIndex();
        } catch (IOException ignored) {
            return;
        }
    }

    private Map<String, ReviewEntry> entries() {
        // Лениво читает сохраненные правки
        if (entries != null) {
            return entries;
        }
        entries = new LinkedHashMap<>();
        try {
            Path index = directory.get().resolve(INDEX);
            if (Files.isRegularFile(index)) {
                for (JsonElement element : JsonParser.parseString(Files.readString(index)).getAsJsonArray()) {
                    JsonObject item = element.getAsJsonObject();
                    String path = item.get("path").getAsString();
                    ReviewBaseline.Kind kind = ReviewBaseline.Kind.valueOf(item.get("baseline").getAsString());
                    entries.put(path, new ReviewEntry(path, new ReviewBaseline(kind, read(path, "base")),
                            read(path, "ai")));
                }
            }
        } catch (IOException | RuntimeException ignored) {
            entries.clear();
        }
        return entries;
    }

    private void writeIndex() throws IOException {
        // Записывает список файлов с правками
        JsonArray index = new JsonArray();
        for (ReviewEntry entry : entries.values()) {
            JsonObject item = new JsonObject();
            item.addProperty("path", entry.path());
            item.addProperty("baseline", entry.baseline().kind().name());
            index.add(item);
        }
        Files.createDirectories(directory.get());
        Files.writeString(directory.get().resolve(INDEX), index.toString(), StandardCharsets.UTF_8);
    }

    private String read(String path, String suffix) throws IOException {
        // Читает сохраненный текст файла
        Path source = file(path, suffix);
        return Files.isRegularFile(source) ? Files.readString(source, StandardCharsets.UTF_8) : "";
    }

    private Path file(String path, String suffix) {
        // Строит имя файла хранилища по пути
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(path.getBytes(StandardCharsets.UTF_8));
            return directory.get().resolve(HexFormat.of().formatHex(hash) + "." + suffix);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
