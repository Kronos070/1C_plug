package com.example.edtai.infrastructure.mock;

import com.example.edtai.domain.Message;
import com.example.edtai.domain.SessionSummary;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MockStore {

    private final Map<String, List<Message>> sessions = new LinkedHashMap<>();
    private final Map<String, Long> updated = new LinkedHashMap<>();
    private String current = newId();

    synchronized String current() {
        // Возвращает путь текущей сессии
        return current;
    }

    synchronized List<Message> messages() {
        // Возвращает сообщения текущей сессии
        return List.copyOf(sessions.getOrDefault(current, List.of()));
    }

    synchronized void add(Message message) {
        // Добавляет сообщение в сессию
        sessions.computeIfAbsent(current, key -> new ArrayList<>()).add(message);
        updated.put(current, System.currentTimeMillis());
    }

    synchronized void startNew() {
        // Начинает новую пустую сессию
        current = newId();
    }

    synchronized void open(String path) {
        // Делает сессию текущей
        if (sessions.containsKey(path)) {
            current = path;
        }
    }

    synchronized List<SessionSummary> summaries() {
        // Возвращает список сохраненных сессий
        List<SessionSummary> result = new ArrayList<>();
        sessions.forEach((path, messages) -> {
            if (!messages.isEmpty()) {
                String title = messages.get(0).plainText().replaceAll("\\s+", " ").strip();
                result.add(new SessionSummary(path, title.length() > 80 ? title.substring(0, 80) + "…" : title,
                        updated.getOrDefault(path, 0L), messages.size()));
            }
        });
        result.sort((left, right) -> Long.compare(right.modified(), left.modified()));
        return result;
    }

    private static String newId() {
        // Генерирует путь демо-сессии
        return "mock://session-" + System.nanoTime();
    }
}
