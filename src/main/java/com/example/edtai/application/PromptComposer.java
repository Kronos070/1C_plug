package com.example.edtai.application;

import com.example.edtai.domain.CodeContext;
import com.example.edtai.domain.CodeProblem;
import java.util.Locale;
import java.util.Optional;

final class PromptComposer {

    private static final String FILE_NOTE = "Файл открыт в редакторе EDT. Прочитай его, если это нужно для ответа.";

    private PromptComposer() {
    }

    static String compose(String text, Optional<CodeContext> context) {
        // Добавляет к сообщению прикрепленный код
        return context.map(value -> text + "\n\n" + block(value)).orElse(text);
    }

    static String compose(String text, Optional<CodeContext> context, String rules) {
        // Оставляет промпт чистым без внедрения правил проекта
        return compose(text, context);
    }

    private static String block(CodeContext context) {
        // Оформляет код в блок контекста
        String file = "<context file=\"" + context.path().replace("\"", "&quot;") + "\"";
        if (!context.hasCode()) {
            return file + ">\n" + FILE_NOTE + problems(context) + "\n</context>";
        }
        String fence = context.code().contains("```") ? "````" : "```";
        return file + " lines=\"" + context.startLine() + "-" + context.endLine() + "\">\n"
                + fence + language(context.path()) + "\n" + context.code().stripTrailing() + "\n" + fence
                + problems(context) + "\n</context>";
    }

    private static String problems(CodeContext context) {
        // Перечисляет замечания EDT к коду
        if (context.problems().isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder("\nЗамечания EDT (ошибки и предупреждения в редакторе):");
        for (CodeProblem problem : context.problems()) {
            text.append("\n- ").append(problem.severity().label()).append(", строка ").append(problem.line())
                    .append(": ").append(problem.message().replace("\n", " "));
        }
        return text.toString();
    }

    private static String language(String path) {
        // Определяет язык по расширению файла
        int dot = path.lastIndexOf('.');
        String extension = dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.ROOT);
        return "os".equals(extension) ? "bsl" : extension;
    }
}
