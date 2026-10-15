package com.example.edtai.infrastructure.auth;

import com.example.edtai.application.port.CredentialStore;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.regex.Pattern;

public final class AuthFileStore implements CredentialStore {

    private static final Pattern PROVIDER = Pattern.compile("[a-z0-9][a-z0-9._-]*");
    private static final String AGENT_DIR_ENV = "PERUN_CODING_AGENT_DIR";

    @Override
    public void saveApiKey(String agentSessionFile, String provider, String key) throws IOException {
        // Записывает ключ в auth.json агента
        if (!PROVIDER.matcher(provider).matches() || key.isBlank()) {
            throw new IOException("Укажите идентификатор провайдера латиницей и непустой ключ");
        }
        Path file = agentDirectory(agentSessionFile).resolve("auth.json");
        Files.createDirectories(file.getParent());
        JsonObject auth = read(file);
        JsonObject credential = new JsonObject();
        credential.addProperty("type", "api_key");
        credential.addProperty("key", key);
        auth.add(provider, credential);
        write(file, auth);
    }

    private static Path agentDirectory(String sessionFile) {
        // Определяет каталог настроек агента
        if (sessionFile != null && !sessionFile.isBlank()) {
            Path folder = Path.of(sessionFile).getParent();
            Path sessions = folder == null ? null : folder.getParent();
            if (sessions != null && "sessions".equals(String.valueOf(sessions.getFileName()))) {
                return sessions.getParent();
            }
        }
        String configured = System.getenv(AGENT_DIR_ENV);
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        return Path.of(System.getProperty("user.home"), ".perun", "agent");
    }

    private static JsonObject read(Path file) throws IOException {
        // Читает существующие ключи агента
        if (!Files.exists(file)) {
            return new JsonObject();
        }
        String text = Files.readString(file, StandardCharsets.UTF_8).replace("﻿", "");
        JsonElement parsed = text.isBlank() ? new JsonObject() : JsonParser.parseString(text);
        if (!parsed.isJsonObject()) {
            throw new IOException("Файл auth.json поврежден: ожидается объект");
        }
        return parsed.getAsJsonObject();
    }

    private static void write(Path file, JsonObject auth) throws IOException {
        // Атомарно сохраняет файл с правами 600
        Path temp = Files.createTempFile(file.getParent(), "auth", ".json");
        try {
            restrict(temp);
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(auth);
            Files.writeString(temp, json, StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static void restrict(Path file) throws IOException {
        // Оставляет доступ только владельцу
        if (file.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        }
    }
}
