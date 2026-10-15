package com.example.edtai.application;

import com.example.edtai.application.port.CredentialStore;
import com.example.edtai.application.port.SessionTools;
import com.example.edtai.domain.ChatEvent;
import com.example.edtai.domain.SessionStats;
import com.example.edtai.domain.SlashCommand;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

final class CommandRunner {

    private final SessionTools tools;
    private final CredentialStore credentials;
    private final Consumer<ChatEvent> emit;
    private final Runnable newChat;
    private final Runnable refresh;

    CommandRunner(SessionTools tools, CredentialStore credentials, Consumer<ChatEvent> emit, Runnable newChat,
            Runnable refresh) {
        this.tools = tools;
        this.credentials = credentials;
        this.emit = emit;
        this.newChat = newChat;
        this.refresh = refresh;
    }

    boolean tryRun(String text) {
        // Выполняет встроенную команду плагина
        if (!text.startsWith("/")) {
            return false;
        }
        String[] parts = text.substring(1).split("\\s+", 2);
        if (BuiltinCommands.plugin(parts[0]).isEmpty()) {
            return false;
        }
        String args = parts.length > 1 ? parts[1].strip() : "";
        switch (parts[0]) {
            case "new" -> newChat.run();
            case "name" -> rename(args);
            case "compact" -> report(tools.compact(args), summary -> summary, "Сжимаю контекст…");
            case "session" -> report(tools.sessionStats(), CommandRunner::describe, "");
            case "export" -> report(tools.exportHtml(), path -> "Чат сохранен: " + path, "");
            default -> {
            }
        }
        return true;
    }

    void publishCommands() {
        // Публикует команды плагина и агента
        tools.commands().whenComplete((agent, error) -> {
            List<SlashCommand> all = new ArrayList<>(BuiltinCommands.ALL);
            if (agent != null) {
                agent.stream()
                        .filter(command -> all.stream().noneMatch(known -> known.name().equals(command.name())))
                        .forEach(all::add);
            }
            emit.accept(new ChatEvent.CommandsListed(all));
        });
    }

    CompletableFuture<Void> login(String sessionFile, String provider, String key) {
        // Сохраняет API-ключ провайдера агента
        return CompletableFuture.runAsync(() -> {
            try {
                credentials.saveApiKey(sessionFile, provider.strip(), key.strip());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).whenComplete((ignored, error) -> emit.accept(error != null
                ? new ChatEvent.Notice("error", "Не удалось сохранить ключ: " + ErrorText.describe(error))
                : new ChatEvent.Notice("info", "Ключ " + provider + " сохранен, перезапускаю агента")));
    }

    private void rename(String name) {
        // Переименовывает текущую сессию
        if (name.isBlank()) {
            emit.accept(new ChatEvent.Notice("warning", "Укажите название: /name Новое название"));
            return;
        }
        CompletableFuture<Void> action = tools.renameSession(name);
        report(action, ignored -> "Чат переименован", "");
        action.thenRun(refresh);
    }

    private <T> void report(CompletableFuture<T> action, Function<T, String> message, String pending) {
        // Сообщает результат команды уведомлением
        if (!pending.isEmpty()) {
            emit.accept(new ChatEvent.Notice("info", pending));
        }
        action.whenComplete((value, error) -> emit.accept(error != null
                ? new ChatEvent.Notice("error", ErrorText.describe(error))
                : new ChatEvent.Notice("info", message.apply(value))));
    }

    private static String describe(SessionStats stats) {
        // Форматирует статистику сессии
        String pattern = "Сообщений: %d / %d · Инструментов: %d · Токенов: %,d · $%.2f · Контекст: %d%%";
        return String.format(Locale.ROOT, pattern, stats.userMessages(), stats.assistantMessages(), stats.toolCalls(),
                stats.tokens(), stats.cost(), stats.contextPercent()).replace(',', ' ');
    }
}
