package com.example.edtai.application;

import com.example.edtai.application.port.CodeReview;
import com.example.edtai.application.port.ReviewFiles;
import com.example.edtai.application.port.ReviewMarks;
import com.example.edtai.application.port.ReviewStore;
import com.example.edtai.domain.ReviewBaseline;
import com.example.edtai.domain.ReviewEntry;
import com.example.edtai.domain.ReviewFile;
import com.example.edtai.domain.UnifiedPatch;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ReviewService implements CodeReview {

    private static final String WRITE_TOOL = "write";

    private final ReviewStore store;
    private final ReviewFiles files;
    private final ReviewMarks marks;
    private final Map<String, ReviewBaseline> starting = new ConcurrentHashMap<>();

    public ReviewService(ReviewStore store, ReviewFiles files, ReviewMarks marks) {
        this.store = store;
        this.files = files;
        this.marks = marks;
    }

    @Override
    public void writing(String path, String tool) {
        // Запоминает файл перед перезаписью агентом
        if (WRITE_TOOL.equals(tool) && store.find(path).isEmpty()) {
            starting.put(path, files.read(path).map(ReviewBaseline::of).orElse(ReviewBaseline.absent()));
        }
    }

    @Override
    public synchronized void written(String path, String tool, String patch) {
        // Пересчитывает правки файла после агента
        Optional<String> current = files.read(path);
        if (current.isEmpty()) {
            return;
        }
        ReviewEntry entry = store.find(path)
                .orElseGet(() -> new ReviewEntry(path, baseline(path, tool, patch, current.get()), ""))
                .withAiText(current.get());
        if (entry.hunks().isEmpty()) {
            store.remove(path);
            marks.clear(path, false);
        } else {
            store.save(entry);
            marks.show(path, entry.hunks());
        }
        publish();
    }

    public synchronized List<ReviewFile> files() {
        // Возвращает файлы с непринятыми правками
        return store.all().stream().map(ReviewFile::of).toList();
    }

    public synchronized void accept(Optional<String> path) {
        // Принимает правки и убирает подсветку
        for (ReviewEntry entry : selected(path)) {
            marks.clear(entry.path(), false);
            store.remove(entry.path());
        }
        publish();
    }

    public synchronized List<String> conflicts(Optional<String> path) {
        // Находит файлы, измененные после агента
        List<String> changed = new ArrayList<>();
        for (ReviewEntry entry : selected(path)) {
            if (!files.read(entry.path()).orElse("").equals(entry.aiText())) {
                changed.add(entry.path());
            }
        }
        return changed;
    }

    public synchronized List<String> reject(Optional<String> path) throws IOException {
        // Откатывает правки агента в файлах
        List<String> skipped = new ArrayList<>();
        for (ReviewEntry entry : selected(path)) {
            switch (entry.baseline().kind()) {
                case TEXT -> files.write(entry.path(), entry.baseline().text());
                case ABSENT -> files.delete(entry.path());
                case UNKNOWN -> skipped.add(entry.path());
            }
            marks.clear(entry.path(), true);
            store.remove(entry.path());
        }
        publish();
        return skipped;
    }

    private ReviewBaseline baseline(String path, String tool, String patch, String current) {
        // Определяет текст файла до первой правки
        ReviewBaseline before = starting.remove(path);
        if (!WRITE_TOOL.equals(tool)) {
            return UnifiedPatch.original(current, patch).map(ReviewBaseline::of).orElse(ReviewBaseline.unknown());
        }
        if (before == null || (before.kind() == ReviewBaseline.Kind.TEXT && before.text().equals(current))) {
            return ReviewBaseline.unknown();
        }
        return before;
    }

    private List<ReviewEntry> selected(Optional<String> path) {
        // Выбирает один файл или все
        return path.map(value -> store.find(value).map(List::of).orElse(List.of())).orElseGet(store::all);
    }

    private void publish() {
        // Сообщает интерфейсу об изменениях
        ReviewSignals.publish(files());
    }
}
