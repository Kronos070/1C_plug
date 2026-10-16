package com.example.edtai.ui.bridge;

import com.example.edtai.application.FeedbackService;
import com.example.edtai.domain.AiProvenance;
import com.example.edtai.domain.ChatEvent;
import com.example.edtai.domain.Feedback;
import com.example.edtai.domain.FeedbackKind;
import java.util.List;
import java.util.function.Consumer;

final class ChatFeedback {

    private final FeedbackService service;
    private final Consumer<ChatEvent> notify;

    ChatFeedback(FeedbackService service, Consumer<ChatEvent> notify) {
        this.service = service;
        this.notify = notify;
    }

    void rate(String rating, String comment, String answer, AiProvenance provenance) {
        // Сохраняет оценку ответа в чате
        FeedbackKind kind = FeedbackKind.fromId(rating).orElse(FeedbackKind.LIKE);
        Feedback entry = new Feedback(kind, "chat", "", 0, 0, List.of(), comment, answer, "", provenance, "");
        service.submit(entry).whenComplete((ignored, error) -> notify.accept(error == null
                ? new ChatEvent.Notice("info", "Спасибо, отзыв сохранен")
                : new ChatEvent.Notice("error", "Не удалось сохранить отзыв: " + error.getMessage())));
    }

    void warn(String message) {
        // Показывает предупреждение в чате
        notify.accept(new ChatEvent.Notice("warning", message));
    }
}
