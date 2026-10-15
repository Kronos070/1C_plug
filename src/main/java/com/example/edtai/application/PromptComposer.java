package com.example.edtai.application;

import com.example.edtai.domain.CodeContext;
import java.util.Locale;
import java.util.Optional;

final class PromptComposer {

    private static final String FILE_NOTE = "Файл открыт в редакторе EDT. Прочитай его, если это нужно для ответа.";

    private PromptComposer() {
    }

    static String compose(String text, Optional<CodeContext> context, String rules) {
        // Добавляет к сообщению правила и прикрепленный код
        String withRules = rules.isEmpty() ? text : text + "\n\n" + rules;
        return context.map(value -> withRules + "\n\n" + block(value)).orElse(withRules);
    }

    private static String block(CodeContext context) {
        // Оформляет код в блок контекста
        String file = "<context file=\"" + context.path().replace("\"", "&quot;") + "\"";
        if (!context.hasCode()) {
            return file + ">\n" + FILE_NOTE + "\n</context>";
        }
        String fence = context.code().contains("```") ? "````" : "```";
        return file + " lines=\"" + context.startLine() + "-" + context.endLine() + "\">\n"
                + fence + language(context.path()) + "\n" + context.code().stripTrailing() + "\n" + fence
                + "\n</context>";
    }

    private static String language(String path) {
        // Определяет язык по расширению файла
        int dot = path.lastIndexOf('.');
        String extension = dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.ROOT);
        return "os".equals(extension) ? "bsl" : extension;
    }
}
