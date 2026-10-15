package com.example.edtai.infrastructure.mock;

import com.example.edtai.application.port.AgentListener;
import com.example.edtai.domain.AgentMode;
import com.example.edtai.domain.ChatEvent;
import com.example.edtai.domain.Message;
import com.example.edtai.domain.MessagePart;
import com.example.edtai.domain.Role;
import com.example.edtai.domain.UiRequest;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

final class MockStream {

    private static final String THINKING = "Нужно найти модуль, прочитать процедуры и кратко объяснить логику.";
    private static final String TOOL_OUTPUT = "Процедуры: ОбработкаПроведения, ОбработкаПроверкиЗаполнения\n"
            + "Функции: РассчитатьСуммуСНДС (Экспорт)";

    private final ScheduledExecutorService scheduler;
    private final AgentListener listener;
    private final BooleanSupplier active;
    private long clock;

    MockStream(ScheduledExecutorService scheduler, AgentListener listener, BooleanSupplier active) {
        this.scheduler = scheduler;
        this.listener = listener;
        this.active = active;
    }

    void play(String prompt, AgentMode mode, Consumer<Message> onDone) {
        // Проигрывает сценарий демо-ответа
        String reply = MockReplies.reply(prompt, mode);
        String toolId = "mock-tool-" + System.nanoTime();
        at(150, () -> listener.onEvent(new ChatEvent.AssistantStarted()));
        streamChunks(THINKING, true);
        at(80, () -> listener.onEvent(
                new ChatEvent.AssistantFinished(message(new MessagePart.Thinking(THINKING)), "")));
        at(120, () -> listener.onEvent(new ChatEvent.ToolStarted(toolId, "1c_ast_symbols",
                "src/Documents/РеализацияТоваров/ObjectModule.bsl")));
        at(700, () -> listener.onEvent(new ChatEvent.ToolFinished(toolId, TOOL_OUTPUT, false)));
        if (mode == AgentMode.WRITE) {
            at(300, () -> listener.onEvent(new ChatEvent.InputRequested(new UiRequest("mock-confirm-" + toolId,
                    UiRequest.Kind.CONFIRM, "[1C Безопасность]: Разрешить вызов инструмента 'edit'?",
                    "Изменение модуля Документ.РеализацияТоваров.МодульОбъекта", List.of(), ""))));
        }
        at(150, () -> listener.onEvent(new ChatEvent.AssistantStarted()));
        streamChunks(reply, false);
        at(120, () -> {
            listener.onEvent(new ChatEvent.AssistantFinished(message(new MessagePart.Text(reply)), ""));
            onDone.accept(message(new MessagePart.Thinking(THINKING),
                    new MessagePart.Tool(toolId, "1c_ast_symbols", "ObjectModule.bsl", TOOL_OUTPUT, false),
                    new MessagePart.Text(reply)));
            listener.onEvent(new ChatEvent.Settled(false));
        });
    }

    private static Message message(MessagePart... parts) {
        // Собирает демо-сообщение ассистента
        return new Message("a-" + System.nanoTime(), Role.ASSISTANT, List.of(parts), System.currentTimeMillis());
    }

    private void streamChunks(String text, boolean thinking) {
        // Режет текст на потоковые дельты
        int position = 0;
        while (position < text.length()) {
            int size = 2 + (position * 7 % 5);
            String chunk = text.substring(position, Math.min(text.length(), position + size));
            position += chunk.length();
            at(18, () -> listener.onEvent(thinking
                    ? new ChatEvent.ThinkingDelta(0, chunk)
                    : new ChatEvent.TextDelta(0, chunk)));
        }
    }

    private void at(long delayMs, Runnable action) {
        // Планирует шаг с учетом отмены
        clock += delayMs;
        MockAgentGateway.schedule(scheduler, clock, () -> {
            if (active.getAsBoolean()) {
                action.run();
            }
        });
    }
}
