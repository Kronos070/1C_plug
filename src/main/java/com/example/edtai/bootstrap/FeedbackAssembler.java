package com.example.edtai.bootstrap;

import com.example.edtai.application.FeedbackService;
import com.example.edtai.application.port.AgentGateway;
import com.example.edtai.application.port.RuleStore;
import com.example.edtai.infrastructure.feedback.MarkdownRuleStore;
import com.example.edtai.infrastructure.settings.FeedbackLocation;

public final class FeedbackAssembler {

    private static volatile AgentGateway gateway;

    private FeedbackAssembler() {
    }

    public static void registerGateway(AgentGateway active) {
        // Регистрирует активный шлюз агента
        gateway = active;
    }

    public static AgentGateway gateway() {
        // Возвращает текущий шлюз агента
        return gateway;
    }

    public static FeedbackService service() {
        // Собирает сервис отзывов с отправкой через RPC
        return new FeedbackService(() -> gateway, rules());
    }

    public static RuleStore rules() {
        // Создает хранилище правил проекта
        return new MarkdownRuleStore(FeedbackLocation::resolve);
    }
}
