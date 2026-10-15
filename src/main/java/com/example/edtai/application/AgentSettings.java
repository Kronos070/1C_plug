package com.example.edtai.application;

import com.example.edtai.application.port.AgentGateway;
import com.example.edtai.domain.AgentMode;
import com.example.edtai.domain.AgentState;
import com.example.edtai.domain.ChatEvent;
import com.example.edtai.domain.ThinkingLevel;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class AgentSettings {

    private final AgentGateway gateway;
    private final Supplier<AgentState> state;
    private final Consumer<ChatEvent> emit;
    private final Runnable refresh;

    AgentSettings(AgentGateway gateway, Supplier<AgentState> state, Consumer<ChatEvent> emit, Runnable refresh) {
        this.gateway = gateway;
        this.state = state;
        this.emit = emit;
        this.refresh = refresh;
    }

    public void selectModel(String key) {
        // Переключает модель по ключу
        AgentState current = state.get();
        if (current == null) {
            return;
        }
        current.models().stream()
                .filter(model -> model.key().equals(key))
                .findFirst()
                .ifPresent(model -> execute(gateway.selectModel(model), true));
    }

    public void selectThinking(String id) {
        // Меняет уровень рассуждений модели
        ThinkingLevel.fromId(id).ifPresent(level -> execute(gateway.selectThinking(level), true));
    }

    public void selectMode(String id) {
        // Меняет рабочий режим агента
        AgentMode.fromId(id).ifPresent(mode -> execute(gateway.selectMode(mode), true));
    }

    void execute(CompletableFuture<Void> action, boolean thenRefresh) {
        // Сообщает ошибку команды и обновляет состояние
        action.whenComplete((ignored, error) -> {
            if (error != null) {
                emit.accept(new ChatEvent.Notice("error", ErrorText.describe(error)));
            }
            if (thenRefresh) {
                refresh.run();
            }
        });
    }
}
