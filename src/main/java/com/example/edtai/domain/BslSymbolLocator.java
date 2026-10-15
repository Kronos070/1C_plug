package com.example.edtai.domain;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BslSymbolLocator {

    private static final String NAME = "([\\p{L}_][\\p{L}\\p{N}_]*)";
    private static final Pattern DECLARATION = Pattern.compile(
            "^\\s*(?:(?:Асинх|Async)\\s+)?(?:Процедура|Функция|Procedure|Function)\\s+" + NAME + "\\s*\\(",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern END = Pattern.compile("^\\s*(?:КонецПроцедуры|КонецФункции|EndProcedure|EndFunction)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern VARIABLE = Pattern.compile("^\\s*(?:Перем|Var)\\s+" + NAME,
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private BslSymbolLocator() {
    }

    public static Optional<String> symbolAt(String[] lines, int line) {
        // Находит метод или переменную у строки
        int index = Math.max(0, Math.min(line, lines.length - 1));
        Matcher variable = VARIABLE.matcher(lines.length == 0 ? "" : lines[index]);
        if (variable.find()) {
            return Optional.of(variable.group(1));
        }
        for (int i = index; i >= 0; i--) {
            Matcher declaration = DECLARATION.matcher(lines[i]);
            if (declaration.find()) {
                return Optional.of(declaration.group(1));
            }
            if (i != index && END.matcher(lines[i]).find()) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static boolean declares(String line, String name) {
        // Проверяет, объявляет ли строка метод
        Matcher declaration = DECLARATION.matcher(line);
        return declaration.find() && declaration.group(1).equalsIgnoreCase(name);
    }

    public static boolean ends(String line) {
        // Проверяет, закрывает ли строка метод
        return END.matcher(line).find();
    }
}
