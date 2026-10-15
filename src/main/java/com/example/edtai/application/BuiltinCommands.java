package com.example.edtai.application;

import com.example.edtai.domain.SlashCommand;
import com.example.edtai.domain.SlashCommand.Kind;
import java.util.List;
import java.util.Optional;

final class BuiltinCommands {

    static final List<SlashCommand> ALL = List.of(
            new SlashCommand("login", "Подключить провайдера по API-ключу", "<провайдер>", Kind.CLIENT),
            new SlashCommand("model", "Выбрать модель", "", Kind.CLIENT),
            new SlashCommand("thinking", "Глубина рассуждений модели", "", Kind.CLIENT),
            new SlashCommand("new", "Начать новый чат", "", Kind.PLUGIN),
            new SlashCommand("history", "История чатов", "", Kind.CLIENT),
            new SlashCommand("name", "Переименовать текущий чат", "<название>", Kind.PLUGIN),
            new SlashCommand("compact", "Сжать контекст диалога", "[что сохранить]", Kind.PLUGIN),
            new SlashCommand("session", "Статистика текущей сессии", "", Kind.PLUGIN),
            new SlashCommand("export", "Сохранить чат в HTML", "", Kind.PLUGIN),
            new SlashCommand("settings", "Настройки плагина", "", Kind.CLIENT),
            new SlashCommand("help", "Список всех команд", "", Kind.CLIENT));

    private BuiltinCommands() {
    }

    static Optional<SlashCommand> plugin(String name) {
        // Ищет команду, выполняемую плагином
        return ALL.stream().filter(command -> command.kind() == Kind.PLUGIN && command.name().equals(name)).findFirst();
    }
}
