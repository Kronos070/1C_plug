package com.example.edtai.infrastructure.rpc;

import com.example.edtai.domain.AgentMode;
import com.example.edtai.domain.AgentState;
import com.example.edtai.domain.ModelInfo;
import com.example.edtai.domain.ThinkingLevel;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

final class RpcStateAssembler {

    private RpcStateAssembler() {
    }

    static CompletableFuture<AgentState> assemble(RpcConnection rpc, AgentMode mode, boolean modesSupported) {
        // Собирает состояние из нескольких запросов
        CompletableFuture<JsonObject> state = rpc.request("get_state", null, true);
        CompletableFuture<JsonObject> models = rpc.request("get_available_models", null, true);
        CompletableFuture<JsonObject> levels = rpc.request("get_available_thinking_levels", null, true);
        return CompletableFuture.allOf(state, models, levels)
                .thenApply(ignored -> build(state.join(), models.join(), levels.join(), mode, modesSupported));
    }

    static boolean supportsModes(JsonObject commands) {
        // Проверяет поддержку команды режимов
        for (JsonElement command : JsonFields.array(commands, "commands")) {
            if ("mode".equals(JsonFields.text(command.getAsJsonObject(), "name"))) {
                return true;
            }
        }
        return false;
    }

    private static AgentState build(JsonObject state, JsonObject models, JsonObject levels, AgentMode mode,
            boolean modesSupported) {
        // Формирует доменное состояние агента
        List<ModelInfo> available = new ArrayList<>();
        for (JsonElement model : JsonFields.array(models, "models")) {
            available.add(RpcMessageParser.model(model.getAsJsonObject()));
        }
        List<ThinkingLevel> thinkingLevels = new ArrayList<>();
        for (JsonElement level : JsonFields.array(levels, "levels")) {
            ThinkingLevel.fromId(level.getAsString()).ifPresent(thinkingLevels::add);
        }
        ModelInfo current = RpcMessageParser.model(JsonFields.object(state, "model"));
        ThinkingLevel thinking = ThinkingLevel.fromId(JsonFields.text(state, "thinkingLevel"))
                .orElse(ThinkingLevel.OFF);
        return new AgentState(current, available, thinking, thinkingLevels, mode, modesSupported,
                JsonFields.flag(state, "isStreaming"), JsonFields.text(state, "sessionFile"),
                JsonFields.text(state, "sessionName"));
    }
}
