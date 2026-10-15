package com.example.edtai.application;

import com.example.edtai.application.port.AgentGateway;
import com.example.edtai.application.port.AgentListener;
import com.example.edtai.application.port.AgentUnavailableException;
import com.example.edtai.application.port.ChatOutput;
import com.example.edtai.application.port.FileFinder;
import com.example.edtai.application.port.PromptOutcome;
import com.example.edtai.application.port.SessionCatalog;
import com.example.edtai.domain.AgentMode;
import com.example.edtai.domain.AgentState;
import com.example.edtai.domain.ChatEvent;
import com.example.edtai.domain.PermissionScope;
import com.example.edtai.domain.UiAnswer;
import java.util.concurrent.CompletableFuture;

public final class ChatService implements AgentListener {

    private static final String MODE_STATUS = "1c-mode";

    private final AgentGateway gateway;
    private final SessionCatalog catalog;
    private final FileFinder files;
    private final ChatOutput output;
    private final CommandRunner commands;
    private final AgentSettings settings;
    private final PermissionGuard guard;
    private volatile AgentState state;
    private volatile boolean stopped;

    public ChatService(ChatPorts ports, ChatOutput output) {
        this.gateway = ports.gateway();
        this.catalog = ports.catalog();
        this.files = ports.files();
        this.output = output;
        this.guard = new PermissionGuard(gateway, ports.permissions(), this::emit);
        this.commands = new CommandRunner(gateway.tools(), ports.credentials(), this::emit, this::newChat,
                this::refreshState);
        this.settings = new AgentSettings(gateway, () -> state, this::emit, this::refreshState);
    }

    public void start() {
        // Запускает агента и грузит состояние
        emit(new ChatEvent.Connecting(gateway.name()));
        try {
            gateway.start(this);
            emit(new ChatEvent.Connected(gateway.name()));
            reload();
            commands.publishCommands();
        } catch (AgentUnavailableException e) {
            emit(new ChatEvent.Disconnected(e.getMessage()));
        }
    }

    public void stop() {
        // Останавливает процесс агента
        stopped = true;
        gateway.stop();
    }

    public void send(String text) {
        // Отправляет сообщение пользователя агенту
        if (text == null || text.isBlank() || commands.tryRun(text.strip())) {
            return;
        }
        gateway.prompt(text.strip()).whenComplete((outcome, error) -> {
            if (error != null) {
                emit(new ChatEvent.Failure(ErrorText.describe(error)));
                emit(new ChatEvent.Settled(false));
            } else if (outcome == PromptOutcome.STARTED) {
                emit(new ChatEvent.Working());
            } else if (outcome == PromptOutcome.HANDLED) {
                emit(new ChatEvent.Settled(false));
                refreshState();
            }
        });
    }

    public void abort() {
        // Прерывает текущий ответ агента
        settings.execute(gateway.abort(), false);
    }

    public void newChat() {
        // Начинает новую сессию чата
        gateway.newSession().whenComplete((ignored, error) -> afterSwitch(error));
    }

    public void openSession(String path) {
        // Открывает сессию из истории
        gateway.openSession(path).whenComplete((ignored, error) -> afterSwitch(error));
    }

    public void listSessions() {
        // Публикует список прошлых сессий
        String current = state == null ? null : state.sessionFile();
        CompletableFuture.supplyAsync(() -> catalog.list(current)).whenComplete((sessions, error) -> {
            if (error != null) {
                emit(new ChatEvent.Notice("error", ErrorText.describe(error)));
            } else {
                emit(new ChatEvent.SessionsListed(sessions, current));
            }
        });
    }

    public void findFiles(String query) {
        // Ищет файлы для упоминания через @
        CompletableFuture.supplyAsync(() -> files.find(query, 30)).whenComplete((paths, error) -> emit(error != null
                ? new ChatEvent.Notice("error", ErrorText.describe(error))
                : new ChatEvent.FilesFound(query, paths)));
    }

    public AgentSettings settings() {
        // Возвращает настройки модели и режима
        return settings;
    }

    public CompletableFuture<Void> login(String provider, String key) {
        // Сохраняет ключ провайдера для агента
        AgentState current = state;
        return commands.login(current == null ? "" : current.sessionFile(), provider, key);
    }

    public void answer(UiAnswer answer, String scope) {
        // Передает ответ и запоминает решение
        guard.remember(answer, PermissionScope.fromId(scope));
        gateway.answer(answer);
    }

    @Override
    public void onEvent(ChatEvent event) {
        // Пробрасывает событие агента в интерфейс
        if (guard.intercept(event)) {
            return;
        }
        emit(event);
        if (event instanceof ChatEvent.Settled) {
            refreshState();
        } else if (event instanceof ChatEvent.StatusChanged status && MODE_STATUS.equals(status.key())) {
            AgentMode.fromStatus(status.text()).ifPresent(this::applyMode);
        }
    }

    private void emit(ChatEvent event) {
        // Отображает событие, пока сервис активен
        if (!stopped) {
            output.render(event);
        }
    }

    private void applyMode(AgentMode mode) {
        // Обновляет режим в состоянии
        AgentState current = state;
        if (current != null && current.mode() != mode) {
            publish(current.withMode(mode));
        }
    }

    private void afterSwitch(Throwable error) {
        // Перечитывает данные после смены сессии
        if (error != null) {
            emit(new ChatEvent.Failure(ErrorText.describe(error)));
            return;
        }
        guard.resetSession();
        reload().thenRun(this::listSessions);
    }

    private CompletableFuture<Void> reload() {
        // Загружает состояние и историю
        CompletableFuture<Void> state = refreshState();
        gateway.messages().whenComplete((messages, error) -> {
            if (error != null) {
                emit(new ChatEvent.Notice("error", ErrorText.describe(error)));
            } else {
                emit(new ChatEvent.HistoryLoaded(messages));
            }
        });
        return state;
    }

    private CompletableFuture<Void> refreshState() {
        // Запрашивает актуальное состояние агента
        return gateway.state().handle((value, error) -> {
            if (error != null) {
                emit(new ChatEvent.Notice("error", ErrorText.describe(error)));
            } else {
                publish(value);
            }
            return null;
        });
    }

    private void publish(AgentState value) {
        // Сохраняет и отображает состояние
        state = value;
        emit(new ChatEvent.StateChanged(value));
    }
}
