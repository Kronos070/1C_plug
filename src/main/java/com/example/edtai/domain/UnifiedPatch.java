package com.example.edtai.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UnifiedPatch {

    private static final Pattern HEADER = Pattern.compile("^@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@");

    private UnifiedPatch() {
    }

    public static Optional<String> original(String current, String patch) {
        // Восстанавливает текст до правки по патчу
        if (patch == null || patch.isBlank()) {
            return Optional.empty();
        }
        TextShape shape = TextShape.of(current);
        List<String> lines = new ArrayList<>(shape.lines());
        List<Hunk> hunks = parse(patch);
        if (hunks.isEmpty()) {
            return Optional.empty();
        }
        for (int i = hunks.size() - 1; i >= 0; i--) {
            Hunk hunk = hunks.get(i);
            int start = Math.max(0, hunk.newStart() - 1);
            if (start + hunk.after().size() > lines.size()
                    || !lines.subList(start, start + hunk.after().size()).equals(hunk.after())) {
                return Optional.empty();
            }
            lines.subList(start, start + hunk.after().size()).clear();
            lines.addAll(start, hunk.before());
        }
        return Optional.of(shape.join(lines));
    }

    private static List<Hunk> parse(String patch) {
        // Разбирает блоки унифицированного патча
        List<Hunk> hunks = new ArrayList<>();
        Hunk current = null;
        for (String line : patch.replace("\r\n", "\n").split("\n", -1)) {
            Matcher header = HEADER.matcher(line);
            if (header.find()) {
                current = new Hunk(Integer.parseInt(header.group(3)), new ArrayList<>(), new ArrayList<>());
                hunks.add(current);
            } else if (current != null && !line.isEmpty()) {
                char sign = line.charAt(0);
                String text = line.substring(1);
                if (sign == ' ' || sign == '-') {
                    current.before().add(text);
                }
                if (sign == ' ' || sign == '+') {
                    current.after().add(text);
                }
            } else if (current != null && line.isEmpty()) {
                current.before().add("");
                current.after().add("");
            }
        }
        hunks.forEach(Hunk::trimTrailingBlank);
        return hunks;
    }

    private record Hunk(int newStart, List<String> before, List<String> after) {

        void trimTrailingBlank() {
            // Убирает пустую строку в конце блока
            if (!before.isEmpty() && !after.isEmpty() && before.get(before.size() - 1).isEmpty()
                    && after.get(after.size() - 1).isEmpty()) {
                before.remove(before.size() - 1);
                after.remove(after.size() - 1);
            }
        }
    }
}
