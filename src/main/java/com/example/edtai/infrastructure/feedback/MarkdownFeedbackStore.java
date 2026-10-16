package com.example.edtai.infrastructure.feedback;

import com.example.edtai.application.port.FeedbackStore;
import com.example.edtai.domain.Feedback;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public final class MarkdownFeedbackStore implements FeedbackStore {

    private final Supplier<Path> root;

    public MarkdownFeedbackStore(Supplier<Path> root) {
        this.root = root;
    }

    @Override
    public void save(Feedback feedback) throws IOException {
        // Записывает отзыв отдельным файлом Markdown
        Path folder = root.get().resolve("feedback");
        Files.createDirectories(folder);
        Path file = folder.resolve(MarkdownText.fileName(feedback.kind().id()));
        Files.writeString(file, render(feedback), StandardCharsets.UTF_8);
    }

    private static String render(Feedback feedback) {
        // Формирует текст файла отзыва
        StringBuilder text = new StringBuilder("---\n");
        text.append("kind: ").append(feedback.kind().id()).append('\n');
        text.append("target: ").append(feedback.target()).append('\n');
        text.append("file: ").append(MarkdownText.quote(feedback.path())).append('\n');
        text.append("lines: ").append(MarkdownText.quote(feedback.startLine() + "-" + feedback.endLine())).append('\n');
        text.append("categories: ").append(MarkdownText.list(feedback.categories())).append('\n');
        text.append("model: ").append(MarkdownText.quote(feedback.provenance().model())).append('\n');
        text.append("source: ").append(MarkdownText.quote(feedback.provenance().source())).append('\n');
        text.append("fragment: ").append(MarkdownText.quote(feedback.fragmentId())).append('\n');
        text.append("author: ").append(MarkdownText.quote(System.getProperty("user.name", ""))).append('\n');
        text.append("created: ").append(MarkdownText.now()).append('\n');
        text.append("status: new\n---\n\n");
        text.append("# ").append(feedback.kind().label());
        if (!feedback.path().isEmpty()) {
            text.append(" — ").append(feedback.path()).append(':').append(feedback.startLine());
        }
        text.append("\n\n");
        section(text, "Комментарий", feedback.comment());
        section(text, "Промпт", feedback.provenance().prompt().isEmpty() ? ""
                : "> " + feedback.provenance().prompt().replace("\n", "\n> "));
        code(text, "Код ИИ", feedback.originalCode(), feedback.path());
        code(text, "Вариант разработчика", feedback.currentCode(), feedback.path());
        return text.toString();
    }

    private static void section(StringBuilder text, String title, String body) {
        // Добавляет раздел, если он не пустой
        if (!body.isBlank()) {
            text.append("## ").append(title).append("\n\n").append(body.strip()).append("\n\n");
        }
    }

    private static void code(StringBuilder text, String title, String code, String path) {
        // Добавляет раздел с блоком кода
        if (!code.isBlank()) {
            section(text, title, MarkdownText.fence(code, path.isEmpty() ? "x.md" : path));
        }
    }
}
