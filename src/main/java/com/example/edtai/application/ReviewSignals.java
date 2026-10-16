package com.example.edtai.application;

import com.example.edtai.domain.ReviewFile;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class ReviewSignals {

    private static final List<Consumer<List<ReviewFile>>> LISTENERS = new CopyOnWriteArrayList<>();

    private ReviewSignals() {
    }

    public static Runnable subscribe(Consumer<List<ReviewFile>> listener) {
        // Подписывает на изменения проверки правок
        LISTENERS.add(listener);
        return () -> LISTENERS.remove(listener);
    }

    static void publish(List<ReviewFile> files) {
        // Рассылает текущие правки подписчикам
        List<ReviewFile> copy = List.copyOf(files);
        LISTENERS.forEach(listener -> listener.accept(copy));
    }
}
