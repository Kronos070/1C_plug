package com.example.edtai.ui.bridge;

import com.example.edtai.domain.AgentMode;
import com.example.edtai.domain.AgentState;
import com.example.edtai.domain.CodeContext;
import com.example.edtai.domain.Message;
import com.example.edtai.domain.MessagePart;
import com.example.edtai.domain.ModelInfo;
import com.example.edtai.domain.SessionSummary;
import com.example.edtai.domain.SlashCommand;
import com.example.edtai.domain.ThinkingLevel;
import com.example.edtai.domain.UiRequest;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Locale;

final class JsonViews {

    private JsonViews() {
    }

    static JsonObject context(CodeContext context) {
        // Описывает прикрепленный код без текста
        JsonObject json = new JsonObject();
        json.addProperty("id", context.id());
        json.addProperty("path", context.path());
        json.addProperty("kind", context.hasCode() ? "selection" : "file");
        json.addProperty("startLine", context.startLine());
        json.addProperty("endLine", context.endLine());
        json.addProperty("problems", context.problems().size());
        return json;
    }

    static JsonObject state(AgentState state) {
        // Сериализует состояние агента для страницы
        JsonObject json = new JsonObject();
        json.add("model", state.model().isKnown() ? model(state.model()) : null);
        JsonArray models = new JsonArray();
        state.models().forEach(item -> models.add(model(item)));
        json.add("models", models);
        json.addProperty("thinking", state.thinking().id());
        JsonArray levels = new JsonArray();
        for (ThinkingLevel level : state.thinkingLevels()) {
            levels.add(option(level.id(), level.label(), ""));
        }
        json.add("thinkingLevels", levels);
        json.addProperty("mode", state.mode() == null ? null : state.mode().id());
        JsonArray modes = new JsonArray();
        if (state.modesSupported()) {
            for (AgentMode mode : AgentMode.values()) {
                modes.add(option(mode.id(), mode.label(), mode.hint()));
            }
        }
        json.add("modes", modes);
        json.addProperty("streaming", state.streaming());
        json.addProperty("sessionName", state.sessionName());
        return json;
    }

    static JsonArray messages(List<Message> messages) {
        // Сериализует список сообщений
        JsonArray array = new JsonArray();
        messages.forEach(message -> array.add(message(message)));
        return array;
    }

    static JsonObject message(Message message) {
        // Сериализует одно сообщение
        JsonObject json = new JsonObject();
        json.addProperty("id", message.id());
        json.addProperty("role", message.role().name().toLowerCase(Locale.ROOT));
        json.addProperty("timestamp", message.timestamp());
        JsonArray parts = new JsonArray();
        message.parts().forEach(part -> parts.add(part(part)));
        json.add("parts", parts);
        return json;
    }

    static JsonArray sessions(List<SessionSummary> sessions) {
        // Сериализует список сессий
        JsonArray array = new JsonArray();
        for (SessionSummary session : sessions) {
            JsonObject json = new JsonObject();
            json.addProperty("path", session.path());
            json.addProperty("title", session.title());
            json.addProperty("modified", session.modified());
            json.addProperty("count", session.messageCount());
            array.add(json);
        }
        return array;
    }

    static JsonArray commands(List<SlashCommand> commands) {
        // Сериализует список слэш-команд
        JsonArray array = new JsonArray();
        for (SlashCommand command : commands) {
            JsonObject json = option(command.name(), command.description(), command.hint());
            json.addProperty("kind", command.kind().name().toLowerCase(Locale.ROOT));
            array.add(json);
        }
        return array;
    }

    static JsonObject request(UiRequest request) {
        // Сериализует запрос ввода от агента
        JsonObject json = new JsonObject();
        json.addProperty("id", request.id());
        json.addProperty("kind", request.kind().name().toLowerCase(Locale.ROOT));
        json.addProperty("title", request.title());
        json.addProperty("message", request.message());
        json.addProperty("prefill", request.prefill());
        JsonArray options = new JsonArray();
        request.options().forEach(options::add);
        json.add("options", options);
        return json;
    }

    private static JsonObject part(MessagePart part) {
        // Сериализует часть сообщения
        JsonObject json = new JsonObject();
        if (part instanceof MessagePart.Text text) {
            json.addProperty("kind", "text");
            json.addProperty("text", text.text());
        } else if (part instanceof MessagePart.Thinking thinking) {
            json.addProperty("kind", "thinking");
            json.addProperty("text", thinking.text());
        } else if (part instanceof MessagePart.Tool tool) {
            json.addProperty("kind", "tool");
            json.addProperty("id", tool.id());
            json.addProperty("name", tool.name());
            json.addProperty("args", tool.arguments());
            json.addProperty("output", tool.output());
            json.addProperty("failed", tool.failed());
        }
        return json;
    }

    private static JsonObject model(ModelInfo model) {
        // Сериализует описание модели
        JsonObject json = new JsonObject();
        json.addProperty("key", model.key());
        json.addProperty("name", model.name());
        json.addProperty("provider", model.provider());
        json.addProperty("reasoning", model.reasoning());
        return json;
    }

    private static JsonObject option(String id, String label, String hint) {
        // Сериализует вариант выбора
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("label", label);
        json.addProperty("hint", hint);
        return json;
    }
}
