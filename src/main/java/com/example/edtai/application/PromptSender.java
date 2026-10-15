package com.example.edtai.application;

import com.example.edtai.application.port.AgentGateway;
import com.example.edtai.application.port.PromptOutcome;
import com.example.edtai.domain.AgentMode;
import com.example.edtai.domain.ChatEvent;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

final class PromptSender {

    private final AgentGateway gateway;
    private final Consumer<ChatEvent> emit;
    private final Runnable refresh;

    PromptSender(AgentGateway gateway, Consumer<ChatEvent> emit, Runnable refresh) {
        this.gateway = gateway;
        this.emit = emit;
        this.refresh = refresh;
    }

    void send(String message, Optional<AgentMode> mode) {
        // Переключает режим и отправляет промпт
        CompletableFuture<Void> ready = mode.map(gateway::selectMode)
                .orElseGet(() -> CompletableFuture.completedFuture(null));
        ready.thenCompose(ignored -> gateway.prompt(message)).whenComplete((outcome, error) -> {
            if (error != null) {
                emit.accept(new ChatEvent.Failure(ErrorText.describe(error)));
                emit.accept(new ChatEvent.Settled(false));
            } else if (outcome == PromptOutcome.STARTED) {
                emit.accept(new ChatEvent.Working());
            } else if (outcome == PromptOutcome.HANDLED) {
                emit.accept(new ChatEvent.Settled(false));
                refresh.run();
            }
        });
    }
}
