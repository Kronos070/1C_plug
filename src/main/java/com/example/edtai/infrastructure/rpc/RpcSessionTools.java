package com.example.edtai.infrastructure.rpc;

import com.example.edtai.application.port.SessionTools;
import com.example.edtai.domain.SessionStats;
import com.example.edtai.domain.SlashCommand;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

final class RpcSessionTools implements SessionTools {

    private final Supplier<RpcConnection> rpc;
    private final Supplier<Path> workDirectory;

    RpcSessionTools(Supplier<RpcConnection> rpc, Supplier<Path> workDirectory) {
        this.rpc = rpc;
        this.workDirectory = workDirectory;
    }

    @Override
    public CompletableFuture<List<SlashCommand>> commands() {
        // Запрашивает слэш-команды агента
        return rpc.get().request("get_commands", null, true).thenApply(RpcCommandParser::commands);
    }

    @Override
    public CompletableFuture<String> compact(String instructions) {
        // Сжимает контекст текущей сессии
        JsonObject payload = instructions.isBlank() ? null : RpcPayloads.of("customInstructions", instructions);
        return rpc.get().request("compact", payload, false).thenApply(RpcCommandParser::compaction);
    }

    @Override
    public CompletableFuture<Void> renameSession(String name) {
        // Задает имя текущей сессии
        return rpc.get().request("set_session_name", RpcPayloads.of("name", name), true).thenApply(data -> null);
    }

    @Override
    public CompletableFuture<String> exportHtml() {
        // Экспортирует сессию в HTML
        return rpc.get().request("export_html", null, true)
                .thenApply(data -> workDirectory.get().resolve(JsonFields.text(data, "path")).normalize().toString());
    }

    @Override
    public CompletableFuture<SessionStats> sessionStats() {
        // Запрашивает статистику сессии
        return rpc.get().request("get_session_stats", null, true).thenApply(RpcCommandParser::stats);
    }
}
