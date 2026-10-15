package com.example.edtai.application;

import com.example.edtai.application.port.AgentGateway;
import com.example.edtai.application.port.AgentUnavailableException;
import com.example.edtai.application.port.RuleStore;
import com.example.edtai.domain.Feedback;
import com.example.edtai.domain.ProjectRule;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class FeedbackService {

    private final Supplier<AgentGateway> gateway;
    private final RuleStore rules;

    public FeedbackService(Supplier<AgentGateway> gateway, RuleStore rules) {
        this.gateway = gateway;
        this.rules = rules;
    }

    public FeedbackService(AgentGateway gateway, RuleStore rules) {
        this(() -> gateway, rules);
    }

    public CompletableFuture<Void> submit(Feedback entry) {
        // Отправляет отзыв агенту через RPC
        AgentGateway current = gateway.get();
        if (current == null) {
            return CompletableFuture.failedFuture(new AgentUnavailableException("Агент не запущен"));
        }
        return current.submitFeedback(entry);
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
