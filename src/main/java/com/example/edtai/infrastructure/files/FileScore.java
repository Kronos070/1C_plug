package com.example.edtai.infrastructure.files;

import java.util.Locale;

final class FileScore {

    private FileScore() {
    }

    static int of(String path, String needle) {
        // Оценивает совпадение пути с запросом
        if (needle.isEmpty()) {
            return path.endsWith(".bsl") ? 2 : 1;
        }
        String lower = path.toLowerCase(Locale.ROOT);
        String name = lower.substring(lower.lastIndexOf('/') + 1);
        if (name.startsWith(needle)) {
            return 100;
        }
        if (name.contains(needle)) {
            return 80;
        }
        if (name.replaceAll("[-_. ]", "").contains(needle)) {
            return 70;
        }
        if (lower.contains(needle)) {
            return 60;
        }
        return isSubsequence(lower, needle) ? 20 : 0;
    }

    private static boolean isSubsequence(String text, String needle) {
        // Проверяет вхождение букв по порядку
        int position = 0;
        for (int i = 0; i < text.length() && position < needle.length(); i++) {
            if (text.charAt(i) == needle.charAt(position)) {
                position++;
            }
        }
        return position == needle.length();
    }
}
