package com.example.edtai.ui.review;

import com.example.edtai.application.ReviewService;
import com.example.edtai.ui.docs.CodeOpener;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

public final class ReviewActions {

    private ReviewActions() {
    }

    public static void accept(ReviewService review, String path) {
        // Принимает правки и снимает подсветку
        review.accept(target(path));
    }

    public static void reject(ReviewService review, String path, Shell shell, BiConsumer<String, String> notice) {
        // Откатывает правки после подтверждения
        Optional<String> target = target(path);
        List<String> conflicts = review.conflicts(target);
        if (!conflicts.isEmpty() && !MessageDialog.openQuestion(shell, "Отменить правки Перуна",
                "После правок Перуна файлы менялись вручную:\n" + names(conflicts)
                        + "\n\nЭти изменения тоже пропадут. Отменить правки?")) {
            return;
        }
        try {
            List<String> skipped = review.reject(target);
            if (!skipped.isEmpty()) {
                notice.accept("warning", "Не удалось откатить: нет исходного текста файла — " + names(skipped));
            } else {
                notice.accept("info", "Правки Перуна отменены");
            }
        } catch (IOException e) {
            notice.accept("error", "Не удалось откатить правки: " + e.getMessage());
        }
    }

    public static void open(String path, int line, BiConsumer<String, String> notice) {
        // Открывает файл на месте правки
        CodeOpener.open(path, line).ifPresent(message -> notice.accept("warning", message));
    }

    private static Optional<String> target(String path) {
        // Выбирает один файл или все
        return path == null || path.isBlank() ? Optional.empty() : Optional.of(path);
    }

    private static String names(List<String> paths) {
        // Перечисляет имена файлов
        return paths.stream().map(path -> Path.of(path).getFileName().toString()).collect(Collectors.joining(", "));
    }
}
