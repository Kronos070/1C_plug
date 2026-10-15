package com.example.edtai.infrastructure.mock;

import com.example.edtai.application.port.AgentGateway;
import com.example.edtai.application.port.AgentListener;
import com.example.edtai.application.port.DocsGateway;
import com.example.edtai.application.port.PromptOutcome;
import com.example.edtai.application.port.SessionTools;
import com.example.edtai.domain.AgentMode;
import com.example.edtai.domain.AgentState;
import com.example.edtai.domain.ChatEvent;
import com.example.edtai.domain.Message;
import com.example.edtai.domain.MessagePart;
import com.example.edtai.domain.ModelInfo;
import com.example.edtai.domain.Role;
import com.example.edtai.domain.ThinkingLevel;
import com.example.edtai.domain.UiAnswer;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class MockAgentGateway implements AgentGateway {

    private static final List<ModelInfo> MODELS = List.of(
            new ModelInfo("demo", "perun-pro", "ПерУН Pro (демо)", true),
            new ModelInfo("demo", "perun-fast", "ПерУН Fast (демо)", false),
            new ModelInfo("demo", "perun-local", "Локальная модель (демо)", false));

    private final MockStore store;
    private final AtomicInteger generation = new AtomicInteger();
    private ScheduledExecutorService scheduler;
    private AgentListener listener;
    private volatile ModelInfo model = MODELS.get(0);
    private volatile ThinkingLevel thinking = ThinkingLevel.MEDIUM;
    private volatile AgentMode mode = AgentMode.EXPLAIN;
    private volatile boolean streaming;

    public MockAgentGateway() {
        this.store = new MockStore();
    }

    public MockSessionCatalog catalog() {
        // Возвращает каталог демо-сессий
        return new MockSessionCatalog(store);
    }

    @Override
    public String name() {
        // Возвращает имя демо-агента
        return "демо-режим";
    }

    @Override
    public void start(AgentListener target) {
        // Готовит демо-агента к работе
        listener = target;
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "perun-mock");
            thread.setDaemon(true);
            return thread;
        });
        listener.onEvent(status(mode));
    }

    @Override
    public void stop() {
        // Останавливает демо-агента
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @Override
    public CompletableFuture<PromptOutcome> prompt(String text) {
        // Имитирует потоковый ответ агента
        store.add(new Message("u-" + System.nanoTime(), Role.USER, List.of(new MessagePart.Text(text)),
                System.currentTimeMillis()));
        int run = generation.incrementAndGet();
        streaming = true;
        new MockStream(scheduler, listener, () -> generation.get() == run).play(text, mode, message -> {
            store.add(message);
            streaming = false;
        });
        return CompletableFuture.completedFuture(PromptOutcome.STARTED);
    }

    @Override
    public CompletableFuture<Void> abort() {
        // Прерывает демо-ответ
        generation.incrementAndGet();
        if (streaming) {
            streaming = false;
            listener.onEvent(new ChatEvent.Settled(true));
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<AgentState> state() {
        // Возвращает состояние демо-агента
        List<ThinkingLevel> levels = model.reasoning()
                ? List.of(ThinkingLevel.OFF, ThinkingLevel.LOW, ThinkingLevel.MEDIUM, ThinkingLevel.HIGH)
                : List.of(ThinkingLevel.OFF);
        ThinkingLevel level = model.reasoning() ? thinking : ThinkingLevel.OFF;
        return CompletableFuture.completedFuture(new AgentState(model, MODELS, level, levels, mode, true, true, streaming,
                store.current(), ""));
    }

    @Override
    public CompletableFuture<List<Message>> messages() {
        // Возвращает историю демо-сессии
        return CompletableFuture.completedFuture(store.messages());
    }

    @Override
    public CompletableFuture<Void> newSession() {
        // Начинает новую демо-сессию
        abort();
        store.startNew();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> openSession(String path) {
        // Открывает демо-сессию из истории
        abort();
        store.open(path);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> selectModel(ModelInfo value) {
        // Выбирает демо-модель
        model = value;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> selectThinking(ThinkingLevel level) {
        // Задает демо-уровень рассуждений
        thinking = level;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> selectMode(AgentMode value) {
        // Переключает демо-режим
        mode = value;
        listener.onEvent(status(value));
        listener.onEvent(new ChatEvent.Notice("info", "Режим агента: " + value.label()));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> submitFeedback(com.example.edtai.domain.Feedback feedback) {
        // Имитирует отправку отзыва
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public DocsGateway docs() {
        // Возвращает демо-документацию
        return MockDocsGateway.shared();
    }

    @Override
    public void answer(UiAnswer answer) {
        // Принимает ответ на демо-запрос
        String verdict = Boolean.TRUE.equals(answer.confirmed()) ? "разрешено" : "отклонено";
        listener.onEvent(new ChatEvent.Notice("info", "Действие " + verdict));
    }

    @Override
    public SessionTools tools() {
        // Возвращает демо-команды сессии
        return new MockSessionTools(store);
    }

    private static ChatEvent status(AgentMode value) {
        // Формирует статус режима как у ПерУН
        return new ChatEvent.StatusChanged("1c-mode", "[1C: " + value.label().toUpperCase(Locale.ROOT) + "]");
    }

    static void schedule(ScheduledExecutorService scheduler, long delayMs, Runnable action) {
        // Планирует шаг демо-ответа
        if (!scheduler.isShutdown()) {
            scheduler.schedule(action, delayMs, TimeUnit.MILLISECONDS);
        }
    }
}
