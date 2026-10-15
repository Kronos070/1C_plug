package com.example.edtai.domain;

import java.util.List;

public record AgentState(
        ModelInfo model,
        List<ModelInfo> models,
        ThinkingLevel thinking,
        List<ThinkingLevel> thinkingLevels,
        AgentMode mode,
        boolean modesSupported,
        boolean streaming,
        String sessionFile,
        String sessionName) {

    public AgentState {
        models = List.copyOf(models);
        thinkingLevels = List.copyOf(thinkingLevels);
    }

    public AgentState withMode(AgentMode value) {
        // Возвращает состояние с режимом
        return new AgentState(model, models, thinking, thinkingLevels, value, modesSupported, streaming,
                sessionFile, sessionName);
    }
}
