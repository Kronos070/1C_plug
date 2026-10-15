package com.example.edtai.infrastructure.marks;

import java.util.List;

final class TextChange {

    private TextChange() {
    }

    static double ratio(String before, String after) {
        // Считает долю измененных строк
        List<String> left = lines(before);
        List<String> right = lines(after);
        if (left.isEmpty() && right.isEmpty()) {
            return 0;
        }
        int[][] table = new int[left.size() + 1][right.size() + 1];
        for (int i = left.size() - 1; i >= 0; i--) {
            for (int j = right.size() - 1; j >= 0; j--) {
                table[i][j] = left.get(i).equals(right.get(j))
                        ? table[i + 1][j + 1] + 1
                        : Math.max(table[i + 1][j], table[i][j + 1]);
            }
        }
        return 1.0 - 2.0 * table[0][0] / (left.size() + right.size());
    }

    private static List<String> lines(String text) {
        // Делит текст на значимые строки
        return text.replace("\r\n", "\n").lines().map(String::strip).filter(line -> !line.isEmpty()).toList();
    }
}
