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
import java.util.concurrent.TimeUnit;

final class RpcStateAssembler {

    private static final long HANDSHAKE_SECONDS = 30;

    private RpcStateAssembler() {
    }

    static CompletableFuture<AgentState> assemble(RpcConnection rpc, AgentMode mode, boolean modes, boolean docs) {
        // Собирает состояние из нескольких запросов
        CompletableFuture<JsonObject> state = rpc.request("get_state", null, true);
        CompletableFuture<JsonObject> models = rpc.request("get_available_models", null, true);
        CompletableFuture<JsonObject> levels = rpc.request("get_available_thinking_levels", null, true);
        return CompletableFuture.allOf(state, models, levels)
                .thenApply(ignored -> build(state.join(), models.join(), levels.join(), mode, modes, docs));
    }

    static JsonObject handshake(RpcConnection rpc) throws Exception {
        // Проверяет связь и получает список команд
        rpc.request("get_state", null, true).get(HANDSHAKE_SECONDS, TimeUnit.SECONDS);
        return rpc.request("get_commands", null, true).get(HANDSHAKE_SECONDS, TimeUnit.SECONDS);
    }

    static boolean supports(JsonObject commands, String name) {
        // Проверяет наличие команды у агента
        for (JsonElement command : JsonFields.array(commands, "commands")) {
            if (name.equals(JsonFields.text(command.getAsJsonObject(), "name"))) {
                return true;
            }
        }
        return false;
    }

    private static AgentState build(JsonObject state, JsonObject models, JsonObject levels, AgentMode mode,
            boolean modesSupported, boolean docsSupported) {
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
        return new AgentState(current, available, thinking, thinkingLevels, mode, modesSupported, docsSupported,
                JsonFields.flag(state, "isStreaming"), JsonFields.text(state, "sessionFile"),
                JsonFields.text(state, "sessionName"));
    }
}
