package com.example.edtai.infrastructure.rpc;

import com.example.edtai.domain.SessionStats;
import com.example.edtai.domain.SlashCommand;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

final class RpcCommandParser {

    private RpcCommandParser() {
    }

    static List<SlashCommand> commands(JsonObject data) {
        // Преобразует список команд агента
        List<SlashCommand> result = new ArrayList<>();
        for (JsonElement element : JsonFields.array(data, "commands")) {
            JsonObject command = element.getAsJsonObject();
            SlashCommand.Kind kind = "extension".equals(JsonFields.text(command, "source"))
                    ? SlashCommand.Kind.COMMAND
                    : SlashCommand.Kind.PROMPT;
            result.add(new SlashCommand(JsonFields.text(command, "name"), JsonFields.text(command, "description"),
                    "", kind));
        }
        return result;
    }

    static SessionStats stats(JsonObject data) {
        // Преобразует статистику сессии
        JsonObject tokens = JsonFields.object(data, "tokens");
        JsonObject context = JsonFields.object(data, "contextUsage");
        double cost = data.has("cost") && data.get("cost").isJsonPrimitive() ? data.get("cost").getAsDouble() : 0;
        return new SessionStats(JsonFields.number(data, "userMessages"), JsonFields.number(data, "assistantMessages"),
                JsonFields.number(data, "toolCalls"), JsonFields.timestamp(tokens, "total"), cost,
                JsonFields.number(context, "percent"));
    }

    static String compaction(JsonObject data) {
        // Описывает результат сжатия контекста
        long before = JsonFields.timestamp(data, "tokensBefore");
        long after = JsonFields.timestamp(data, "estimatedTokensAfter");
        return before > 0 ? "Контекст сжат: " + before + " → " + after + " токенов" : "Контекст сжат";
    }
}
