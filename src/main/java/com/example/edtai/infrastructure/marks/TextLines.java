package com.example.edtai.infrastructure.marks;

import java.util.ArrayList;
import java.util.List;

public final class TextLines {

    private TextLines() {
    }

    public static List<int[]> offsets(String text) {
        // Рассчитывает начальные и конечные смещения строк
        List<int[]> lines = new ArrayList<>();
        int len = text.length();
        int start = 0;
        int i = 0;
        while (i < len) {
            char c = text.charAt(i);
            if (c == '\r' || c == '\n') {
                int end = c == '\r' && i + 1 < len && text.charAt(i + 1) == '\n' ? i + 2 : i + 1;
                lines.add(new int[] {start, end});
                i = end;
                start = end;
            } else {
                i++;
            }
        }
        if (start < len || lines.isEmpty()) {
            lines.add(new int[] {start, len});
        }
        return lines;
    }
}
