package com.example.edtai.infrastructure.mock;

import com.example.edtai.application.port.SessionTools;
import com.example.edtai.domain.SessionStats;
import com.example.edtai.domain.SlashCommand;
import com.example.edtai.domain.SlashCommand.Kind;
import java.util.List;
import java.util.concurrent.CompletableFuture;

final class MockSessionTools implements SessionTools {

    private static final List<SlashCommand> COMMANDS = List.of(
            new SlashCommand("mode", "Режим агента 1С: /mode [find | explain | write | plan]", "", Kind.COMMAND),
            new SlashCommand("permission", "Политика подтверждений: /permission [strict | auto-approve | yolo]", "",
                    Kind.COMMAND),
            new SlashCommand("doctor", "Диагностика тулчейна 1С", "", Kind.COMMAND),
            new SlashCommand("skill:interface-validate", "Проверка командного интерфейса 1С", "", Kind.PROMPT),
            new SlashCommand("skill:meta-info", "Сведения об объекте метаданных", "", Kind.PROMPT));

    private final MockStore store;

    MockSessionTools(MockStore store) {
        this.store = store;
    }

    @Override
    public CompletableFuture<List<SlashCommand>> commands() {
        // Возвращает демо-команды агента
        return CompletableFuture.completedFuture(COMMANDS);
    }

    @Override
    public CompletableFuture<String> compact(String instructions) {
        // Имитирует сжатие контекста
        return CompletableFuture.completedFuture("Контекст сжат (демо)");
    }

    @Override
    public CompletableFuture<Void> renameSession(String name) {
        // Имитирует переименование чата
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<String> exportHtml() {
        // Имитирует экспорт чата
        return CompletableFuture.completedFuture(System.getProperty("java.io.tmpdir") + "/perun-demo.html");
    }

    @Override
    public CompletableFuture<SessionStats> sessionStats() {
        // Возвращает демо-статистику сессии
        int count = store.messages().size();
        return CompletableFuture.completedFuture(new SessionStats(count / 2, count - count / 2, 0, count * 350L, 0, 3));
    }
}
