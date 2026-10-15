package com.example.edtai.infrastructure.rpc;

import com.example.edtai.domain.ChatEvent;
import com.example.edtai.domain.UiRequest;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class RpcEventTranslator {

    private static final Pattern ANSI = Pattern.compile("\u001B\\[[0-9;?]*[ -/]*[@-~]");

    public Optional<ChatEvent> translate(JsonObject event) {
        // Переводит событие RPC в событие чата
        return Optional.ofNullable(switch (JsonFields.text(event, "type")) {
            case "message_start" -> isAssistant(event) ? new ChatEvent.AssistantStarted() : null;
            case "message_update" -> delta(JsonFields.object(event, "assistantMessageEvent"));
            case "message_end" -> isAssistant(event) ? finished(JsonFields.object(event, "message")) : null;
            case "tool_execution_start" -> new ChatEvent.ToolStarted(JsonFields.text(event, "toolCallId"),
                    JsonFields.text(event, "toolName"), ToolArguments.summarize(JsonFields.object(event, "args")));
            case "tool_execution_end" -> new ChatEvent.ToolFinished(JsonFields.text(event, "toolCallId"),
                    ToolArguments.output(JsonFields.object(event, "result")), JsonFields.flag(event, "isError"));
            case "agent_settled" -> new ChatEvent.Settled(JsonFields.flag(event, "aborted"));
            case "auto_retry_start" -> new ChatEvent.Notice("warning", "Повтор запроса к модели: попытка "
                    + JsonFields.number(event, "attempt") + " из " + JsonFields.number(event, "maxAttempts"));
            case "compaction_start" -> new ChatEvent.Notice("info", "Сжатие контекста диалога…");
            case "extension_error" -> new ChatEvent.Notice("error", JsonFields.text(event, "error"));
            case "extension_ui_request" -> uiRequest(event);
            default -> null;
        });
    }

    private static boolean isAssistant(JsonObject event) {
        // Проверяет, что сообщение от ассистента
        return "assistant".equals(JsonFields.text(JsonFields.object(event, "message"), "role"));
    }

    private static ChatEvent delta(JsonObject update) {
        // Переводит потоковую дельту ответа
        int index = JsonFields.number(update, "contentIndex");
        return switch (JsonFields.text(update, "type")) {
            case "text_delta" -> new ChatEvent.TextDelta(index, JsonFields.text(update, "delta"));
            case "thinking_delta" -> new ChatEvent.ThinkingDelta(index, JsonFields.text(update, "delta"));
            default -> null;
        };
    }

    private static ChatEvent finished(JsonObject message) {
        // Формирует итоговое сообщение ассистента
        String error = "error".equals(JsonFields.text(message, "stopReason"))
                ? JsonFields.text(message, "errorMessage")
                : "";
        return new ChatEvent.AssistantFinished(RpcMessageParser.assistant("live", message, Map.of()), error);
    }

    private static ChatEvent uiRequest(JsonObject event) {
        // Переводит запрос расширения к интерфейсу
        String id = JsonFields.text(event, "id");
        String title = JsonFields.text(event, "title");
        return switch (JsonFields.text(event, "method")) {
            case "notify" -> new ChatEvent.Notice(level(event), clean(JsonFields.text(event, "message")));
            case "setStatus" -> new ChatEvent.StatusChanged(JsonFields.text(event, "statusKey"),
                    clean(JsonFields.text(event, "statusText")));
            case "set_editor_text" -> new ChatEvent.EditorTextSet(JsonFields.text(event, "text"));
            case "confirm" -> request(id, UiRequest.Kind.CONFIRM, title, JsonFields.text(event, "message"),
                    List.of(), "");
            case "select" -> request(id, UiRequest.Kind.SELECT, title, "", options(event), "");
            case "input" -> request(id, UiRequest.Kind.INPUT, title, JsonFields.text(event, "placeholder"),
                    List.of(), "");
            case "editor" -> request(id, UiRequest.Kind.EDITOR, title, "", List.of(),
                    JsonFields.text(event, "prefill"));
            default -> null;
        };
    }

    private static ChatEvent request(String id, UiRequest.Kind kind, String title, String message,
            List<String> options, String prefill) {
        // Создает событие запроса ввода
        return new ChatEvent.InputRequested(new UiRequest(id, kind, clean(title), clean(message), options, prefill));
    }

    private static List<String> options(JsonObject event) {
        // Читает варианты выбора
        List<String> result = new ArrayList<>();
        for (JsonElement option : JsonFields.array(event, "options")) {
            result.add(clean(option.getAsString()));
        }
        return result;
    }

    private static String level(JsonObject event) {
        // Определяет уровень уведомления
        String level = JsonFields.text(event, "notifyType");
        return level.isBlank() ? "info" : level;
    }

    private static String clean(String text) {
        // Удаляет ANSI-коды из текста
        return ANSI.matcher(text == null ? "" : text).replaceAll("").strip();
    }
}
