package com.example.edtai.bootstrap;

import com.example.edtai.application.FeedbackService;
import com.example.edtai.application.port.RuleStore;
import com.example.edtai.infrastructure.feedback.MarkdownFeedbackStore;
import com.example.edtai.infrastructure.feedback.MarkdownRuleStore;
import com.example.edtai.infrastructure.settings.FeedbackLocation;

public final class FeedbackAssembler {

    private FeedbackAssembler() {
    }

    public static FeedbackService service() {
        // Собирает сервис отзывов с хранилищем Markdown
        return new FeedbackService(new MarkdownFeedbackStore(FeedbackLocation::resolve), rules());
    }

    public static RuleStore rules() {
        // Создает хранилище правил проекта
        return new MarkdownRuleStore(FeedbackLocation::resolve);
    }
}
