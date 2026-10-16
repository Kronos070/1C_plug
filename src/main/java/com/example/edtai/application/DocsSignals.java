package com.example.edtai.application;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class DocsSignals {

    public static final String RECONNECTED = "";
    public static final String UPDATED = "docs:updated";

    private static final List<Consumer<String>> LISTENERS = new CopyOnWriteArrayList<>();

    private DocsSignals() {
    }

    public static Runnable subscribe(Consumer<String> listener) {
        // Подписывает на статус документации
        LISTENERS.add(listener);
        return () -> LISTENERS.remove(listener);
    }

    public static void publish(String status) {
        // Рассылает статус индексации подписчикам
        LISTENERS.forEach(listener -> listener.accept(status));
    }
}
