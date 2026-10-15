package com.example.edtai.ui.bridge;

import com.example.edtai.domain.AgentMode;
import com.example.edtai.domain.ChatEvent;
import com.example.edtai.domain.CodeAction;
import com.example.edtai.domain.CodeContext;
import com.example.edtai.ui.theme.EdtPalette;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.util.Optional;

final class EventSerializer {

    private EventSerializer() {
    }

    static JsonObject toJson(ChatEvent event) {
        // Переводит событие чата в JSON страницы
        if (event instanceof ChatEvent.Connecting value) {
            return typed("connecting").with("agent", value.agent()).json;
        } else if (event instanceof ChatEvent.Connected value) {
            return typed("connected").with("agent", value.agent()).json;
        } else if (event instanceof ChatEvent.Disconnected value) {
            return typed("disconnected").with("reason", value.reason()).json;
        } else if (event instanceof ChatEvent.StateChanged value) {
            return typed("state").put("state", JsonViews.state(value.state())).json;
        } else if (event instanceof ChatEvent.HistoryLoaded value) {
            return typed("history").put("messages", JsonViews.messages(value.messages())).json;
        } else if (event instanceof ChatEvent.SessionsListed value) {
            return typed("sessions").put("items", JsonViews.sessions(value.sessions()))
                    .with("current", value.current()).json;
        } else if (event instanceof ChatEvent.CommandsListed value) {
            return typed("commands").put("items", JsonViews.commands(value.commands())).json;
        } else if (event instanceof ChatEvent.FilesFound value) {
            JsonArray items = new JsonArray();
            value.paths().forEach(items::add);
            return typed("files").with("query", value.query()).put("items", items).json;
        } else if (event instanceof ChatEvent.TextDelta value) {
            return typed("textDelta").with("index", value.index()).with("delta", value.delta()).json;
        } else if (event instanceof ChatEvent.ThinkingDelta value) {
            return typed("thinkingDelta").with("index", value.index()).with("delta", value.delta()).json;
        } else if (event instanceof ChatEvent.ToolStarted value) {
            return typed("toolStart").with("id", value.id()).with("name", value.name())
                    .with("args", value.arguments()).json;
        } else if (event instanceof ChatEvent.ToolFinished value) {
            return typed("toolEnd").with("id", value.id()).with("output", value.output())
                    .with("failed", value.failed()).json;
        } else if (event instanceof ChatEvent.AssistantFinished value) {
            return typed("assistantEnd").put("message", JsonViews.message(value.message()))
                    .with("error", value.error()).json;
        } else if (event instanceof ChatEvent.Settled value) {
            return typed("settled").with("aborted", value.aborted()).json;
        } else if (event instanceof ChatEvent.Failure value) {
            return typed("failure").with("message", value.message()).json;
        } else if (event instanceof ChatEvent.Notice value) {
            return typed("notice").with("level", value.level()).with("text", value.text()).json;
        } else if (event instanceof ChatEvent.StatusChanged value) {
            return typed("status").with("key", value.key()).with("text", value.text()).json;
        } else if (event instanceof ChatEvent.InputRequested value) {
            return typed("request").put("request", JsonViews.request(value.request())).json;
        } else if (event instanceof ChatEvent.EditorTextSet value) {
            return typed("editorText").with("text", value.text()).json;
        }
        return typed(event instanceof ChatEvent.Working ? "working" : "assistantStart").json;
    }

    static JsonObject theme(EdtPalette palette) {
        // Переводит палитру EDT в JSON
        return typed("theme").with("dark", palette.dark()).with("background", palette.background())
                .with("foreground", palette.foreground()).json;
    }

    static JsonObject context(Optional<CodeContext> context) {
        // Переводит прикрепленный код в JSON
        return typed("context").put("context", context.<JsonElement>map(JsonViews::context)
                .orElse(JsonNull.INSTANCE)).json;
    }

    static JsonObject ask(CodeAction action, CodeContext context) {
        // Переводит действие с кодом в JSON
        return typed("ask").with("text", action.prompt()).with("mode", action.mode().map(AgentMode::id).orElse(""))
                .put("context", JsonViews.context(context)).json;
    }

    private static Builder typed(String type) {
        // Создает JSON с типом события
        return new Builder().with("type", type);
    }

    private static final class Builder {

        private final JsonObject json = new JsonObject();

        Builder with(String key, String value) {
            // Добавляет строковое поле
            json.addProperty(key, value);
            return this;
        }

        Builder with(String key, Number value) {
            // Добавляет числовое поле
            json.addProperty(key, value);
            return this;
        }

        Builder with(String key, boolean value) {
            // Добавляет логическое поле
            json.addProperty(key, value);
            return this;
        }

        Builder put(String key, JsonElement value) {
            // Добавляет вложенный элемент
            json.add(key, value);
            return this;
        }
    }
}
