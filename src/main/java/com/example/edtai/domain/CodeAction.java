package com.example.edtai.domain;

import java.util.Arrays;
import java.util.Optional;

public enum CodeAction {
    EXPLAIN("explain", "Объясни, что делает этот код: его назначение, основную логику и используемые объекты "
            + "конфигурации. Ничего не изменяй.", null),
    DOCUMENT("document", "Добавь к процедурам и функциям из этого кода документирующие комментарии по стандарту "
            + "1С: описание, разделы «Параметры:» и «Возвращаемое значение:». Внеси комментарии прямо в модуль, "
            + "логику кода не меняй.", AgentMode.WRITE),
    REVIEW("review", "Найди ошибки и проблемы в этом коде: логические ошибки, запросы в цикле, лишние обращения "
            + "к базе, нарушения стандартов разработки 1С. Ничего не изменяй — перечисли проблемы с пояснением "
            + "и вариантом исправления.", null),
    REFACTOR("refactor", "Улучши этот код: сделай его понятнее и быстрее, соблюдая стандарты разработки 1С. "
            + "Поведение должно остаться прежним. Внеси изменения прямо в модуль и кратко опиши, что изменено.",
            AgentMode.WRITE);

    private final String id;
    private final String prompt;
    private final AgentMode mode;

    CodeAction(String id, String prompt, AgentMode mode) {
        this.id = id;
        this.prompt = prompt;
        this.mode = mode;
    }

    public String prompt() {
        // Возвращает текст запроса к агенту
        return prompt;
    }

    public Optional<AgentMode> mode() {
        // Возвращает требуемый режим агента
        return Optional.ofNullable(mode);
    }

    public static Optional<CodeAction> fromId(String value) {
        // Ищет действие по идентификатору
        return Arrays.stream(values()).filter(action -> action.id.equals(value)).findFirst();
    }
}
