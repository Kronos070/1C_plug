package com.example.edtai.infrastructure.rpc;

import com.example.edtai.application.port.AgentGateway;
import com.example.edtai.application.port.AgentListener;
import com.example.edtai.application.port.AgentUnavailableException;
import com.example.edtai.application.port.DocsGateway;
import com.example.edtai.application.port.PromptOutcome;
import com.example.edtai.application.port.SessionTools;
import com.example.edtai.domain.AgentMode;
import com.example.edtai.domain.AgentState;
import com.example.edtai.domain.ChatEvent;
import com.example.edtai.domain.Feedback;
import com.example.edtai.domain.Message;
import com.example.edtai.domain.ModelInfo;
import com.example.edtai.domain.ThinkingLevel;
import com.example.edtai.domain.UiAnswer;
import com.example.edtai.infrastructure.process.AgentProcess;
import com.example.edtai.infrastructure.process.LaunchSpec;
import com.example.edtai.infrastructure.process.LaunchSpecSource;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class PerunGateway implements AgentGateway {

    private static final String MODE_STATUS = "1c-mode";

    private final LaunchSpecSource source;
    private final RpcEventTranslator translator = new RpcEventTranslator();
    private final RpcWriteTracker writes = new RpcWriteTracker();
    private final RpcDocsGateway docs = new RpcDocsGateway(() -> this.rpc, () -> this.spec);
    private volatile LaunchSpec spec;
    private volatile AgentProcess process;
    private volatile RpcConnection rpc;
    private volatile AgentListener listener;
    private volatile AgentMode mode;
    private volatile boolean modesSupported;
    private volatile boolean stopping;

    public PerunGateway(LaunchSpecSource source) {
        this.source = source;
    }

    @Override
    public String name() {
        // Возвращает имя запущенного агента
        LaunchSpec current = spec;
        return current == null ? "perun" : current.displayName();
    }

    @Override
    public void start(AgentListener target) throws AgentUnavailableException {
        // Запускает процесс и проверяет связь
        listener = target;
        spec = source.resolve();
        try {
            process = AgentProcess.start(spec);
        } catch (IOException e) {
            throw new AgentUnavailableException("Не удалось запустить «" + String.join(" ", spec.command()) + "»: "
                    + e.getMessage(), e);
        }
        if (stopping) {
            process.destroy();
            throw new AgentUnavailableException("Запуск агента отменен");
        }
        rpc = new RpcConnection(process);
        rpc.open(this::onRecord, this::onClosed);
        try {
            JsonObject commands = RpcStateAssembler.handshake(rpc);
            modesSupported = RpcStateAssembler.supports(commands, "mode");
            docs.support(RpcStateAssembler.supports(commands, "docs"));
        } catch (Exception e) {
            String details = process.stderrTail();
            stop();
            throw new AgentUnavailableException("Агент не ответил на запрос состояния."
                    + (details.isBlank() ? "" : "\n" + details), e);
        }
    }

    @Override
    public void stop() {
        // Останавливает процесс агента
        stopping = true;
        if (process != null) {
            process.destroy();
        }
    }

    @Override
    public CompletableFuture<PromptOutcome> prompt(String text) {
        // Отправляет промпт агенту
        JsonObject payload = RpcPayloads.of("message", text, "streamingBehavior", "followUp");
        return rpc.request("prompt", payload, false).thenApply(data -> switch (JsonFields.text(data, "disposition")) {
            case "queued" -> PromptOutcome.QUEUED;
            case "handled" -> PromptOutcome.HANDLED;
            default -> PromptOutcome.STARTED;
        });
    }

    @Override
    public CompletableFuture<Void> abort() {
        // Прерывает работу агента
        return command("abort", null);
    }

    @Override
    public CompletableFuture<AgentState> state() {
        // Запрашивает состояние агента
        return RpcStateAssembler.assemble(rpc, mode, modesSupported, docs.supported());
    }

    @Override
    public CompletableFuture<List<Message>> messages() {
        // Запрашивает историю текущей сессии
        return rpc.request("get_messages", null, true)
                .thenApply(data -> RpcMessageParser.parseAll(JsonFields.array(data, "messages")));
    }

    @Override
    public CompletableFuture<Void> newSession() {
        // Создает новую сессию агента
        return command("new_session", null);
    }

    @Override
    public CompletableFuture<Void> openSession(String path) {
        // Переключает агента на сессию
        return command("switch_session", RpcPayloads.of("sessionPath", path));
    }

    @Override
    public CompletableFuture<Void> selectModel(ModelInfo model) {
        // Выбирает модель агента
        return command("set_model", RpcPayloads.of("provider", model.provider(), "modelId", model.id()));
    }

    @Override
    public CompletableFuture<Void> selectThinking(ThinkingLevel level) {
        // Задает уровень рассуждений
        return command("set_thinking_level", RpcPayloads.of("level", level.id()));
    }

    @Override
    public CompletableFuture<Void> selectMode(AgentMode value) {
        // Переключает режим через слэш-команду
        return prompt("/mode " + value.id()).thenAccept(outcome -> mode = value);
    }

    @Override
    public CompletableFuture<Void> submitFeedback(Feedback feedback) {
        // Отправляет отзыв о коде агенту через RPC
        return rpc == null ? CompletableFuture.failedFuture(new AgentUnavailableException("Агент не запущен"))
                : command("submit_feedback", RpcPayloads.feedback(feedback));
    }

    @Override
    public DocsGateway docs() {
        // Возвращает документацию из Перуна
        return docs;
    }

    @Override
    public void answer(UiAnswer answer) {
        // Отвечает на запрос расширения
        rpc.send(RpcPayloads.answer(answer));
    }

    @Override
    public SessionTools tools() {
        // Возвращает служебные команды сессии
        return new RpcSessionTools(() -> rpc, () -> spec.workDirectory());
    }

    private CompletableFuture<Void> command(String type, JsonObject payload) {
        // Выполняет команду без данных ответа
        return rpc.request(type, payload, true).thenApply(data -> null);
    }

    private void onRecord(JsonObject record) {
        // Обрабатывает событие от агента
        translator.translate(record).ifPresent(event -> {
            if (event instanceof ChatEvent.StatusChanged status && MODE_STATUS.equals(status.key())) {
                AgentMode.fromStatus(status.text()).ifPresent(value -> mode = value);
            }
            listener.onEvent(event);
        });
        writes.track(record, spec.workDirectory()).ifPresent(listener::onEvent);
    }

    private void onClosed() {
        // Сообщает о неожиданном завершении
        if (!stopping) {
            String details = process.stderrTail();
            String reason = "Агент завершил работу." + (details.isBlank() ? "" : "\n" + details);
            listener.onEvent(new ChatEvent.Disconnected(reason));
        }
    }
}
