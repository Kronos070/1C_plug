package com.example.edtai.infrastructure.rpc;

import com.example.edtai.domain.Message;
import com.example.edtai.domain.MessagePart;
import com.example.edtai.domain.ModelInfo;
import com.example.edtai.domain.Role;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RpcMessageParser {

    private RpcMessageParser() {
    }

    public static List<Message> parseAll(JsonArray messages) {
        // Преобразует историю агента в сообщения
        Map<String, JsonObject> results = new HashMap<>();
        for (JsonElement element : messages) {
            JsonObject message = element.getAsJsonObject();
            if ("toolResult".equals(JsonFields.text(message, "role"))) {
                results.put(JsonFields.text(message, "toolCallId"), message);
            }
        }
        List<Message> parsed = new ArrayList<>();
        for (JsonElement element : messages) {
            JsonObject message = element.getAsJsonObject();
            String role = JsonFields.text(message, "role");
            String id = "h-" + parsed.size();
            if ("user".equals(role)) {
                parsed.add(user(id, message));
            } else if ("assistant".equals(role)) {
                parsed.add(assistant(id, message, results));
            }
        }
        return parsed;
    }

    public static Message assistant(String id, JsonObject message, Map<String, JsonObject> results) {
        // Собирает сообщение ассистента из блоков
        List<MessagePart> parts = new ArrayList<>();
        for (JsonElement element : JsonFields.array(message, "content")) {
            JsonObject block = element.getAsJsonObject();
            switch (JsonFields.text(block, "type")) {
                case "text" -> parts.add(new MessagePart.Text(JsonFields.text(block, "text")));
                case "thinking" -> parts.add(new MessagePart.Thinking(JsonFields.text(block, "thinking")));
                case "toolCall" -> parts.add(tool(block, results.get(JsonFields.text(block, "id"))));
                default -> {
                }
            }
        }
        return new Message(id, Role.ASSISTANT, parts, JsonFields.timestamp(message, "timestamp"));
    }

    public static ModelInfo model(JsonObject model) {
        // Преобразует описание модели агента
        String id = JsonFields.text(model, "id");
        String name = JsonFields.text(model, "name");
        return new ModelInfo(JsonFields.text(model, "provider"), id, name.isBlank() ? id : name,
                JsonFields.flag(model, "reasoning"));
    }

    private static Message user(String id, JsonObject message) {
        // Собирает сообщение пользователя
        JsonElement content = message.get("content");
        String text = content != null && content.isJsonArray()
                ? ToolArguments.joinText(content.getAsJsonArray())
                : JsonFields.text(message, "content");
        long timestamp = JsonFields.timestamp(message, "timestamp");
        return new Message(id, Role.USER, List.of(new MessagePart.Text(text)), timestamp);
    }

    private static MessagePart tool(JsonObject call, JsonObject result) {
        // Собирает часть с вызовом инструмента
        String output = result == null ? "" : ToolArguments.output(result);
        boolean failed = result != null && JsonFields.flag(result, "isError");
        return new MessagePart.Tool(JsonFields.text(call, "id"), JsonFields.text(call, "name"),
                ToolArguments.summarize(JsonFields.object(call, "arguments")), output, failed);
    }
}
