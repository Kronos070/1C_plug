package com.example.edtai.application;

import com.example.edtai.application.port.FeedbackStore;
import com.example.edtai.application.port.RuleStore;
import com.example.edtai.domain.Feedback;
import com.example.edtai.domain.ProjectRule;
import java.io.IOException;
import java.util.List;

public final class FeedbackService {

    private final FeedbackStore feedback;
    private final RuleStore rules;

    public FeedbackService(FeedbackStore feedback, RuleStore rules) {
        this.feedback = feedback;
        this.rules = rules;
    }

    public void submit(Feedback entry) throws IOException {
        // Сохраняет отзыв в хранилище
        feedback.save(entry);
    }

    public void addRule(ProjectRule rule) throws IOException {
        // Сохраняет новое правило проекта
        if (!rule.text().isBlank()) {
            rules.save(rule);
        }
    }

    public List<ProjectRule> rules() {
        // Возвращает все правила проекта
        return rules.rules();
    }
}
