package com.example.edtai.infrastructure.process;

import java.util.ArrayList;
import java.util.List;

public final class CommandLine {

    private CommandLine() {
    }

    public static List<String> split(String line) {
        // Разбивает строку на аргументы
        List<String> result = new ArrayList<>();
        if (line == null) {
            return result;
        }
        StringBuilder current = new StringBuilder();
        char quote = 0;
        boolean pending = false;
        for (char symbol : line.toCharArray()) {
            if (quote != 0) {
                if (symbol == quote) {
                    quote = 0;
                } else {
                    current.append(symbol);
                }
            } else if (symbol == '"' || symbol == '\'') {
                quote = symbol;
                pending = true;
            } else if (Character.isWhitespace(symbol)) {
                if (pending || current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                    pending = false;
                }
            } else {
                current.append(symbol);
            }
        }
        if (pending || current.length() > 0) {
            result.add(current.toString());
        }
        return result;
    }
}
