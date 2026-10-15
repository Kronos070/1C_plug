package com.example.edtai.domain;

import java.util.ArrayList;
import java.util.List;

public final class LineDiff {

    private static final int MAX_EDITS = 4000;
    private static final long TRACE_BUDGET = 16_000_000L;

    private LineDiff() {
    }

    public static List<DiffHunk> diff(List<String> before, List<String> after) {
        // Находит измененные блоки строк алгоритмом Майерса
        int prefix = 0;
        while (prefix < before.size() && prefix < after.size() && before.get(prefix).equals(after.get(prefix))) {
            prefix++;
        }
        int suffix = 0;
        while (suffix < before.size() - prefix && suffix < after.size() - prefix
                && before.get(before.size() - 1 - suffix).equals(after.get(after.size() - 1 - suffix))) {
            suffix++;
        }
        List<String> a = before.subList(prefix, before.size() - suffix);
        List<String> b = after.subList(prefix, after.size() - suffix);
        if (a.isEmpty() && b.isEmpty()) {
            return List.of();
        }
        boolean[] keptA = new boolean[a.size()];
        boolean[] keptB = new boolean[b.size()];
        if (!match(a, b, keptA, keptB)) {
            return List.of(new DiffHunk(prefix, a, prefix, b.size()));
        }
        return hunks(a, keptA, keptB, prefix);
    }

    private static boolean match(List<String> a, List<String> b, boolean[] keptA, boolean[] keptB) {
        // Отмечает общие строки по кратчайшему пути правок
        int n = a.size();
        int m = b.size();
        int offset = n + m + 1;
        int[] v = new int[2 * offset + 1];
        List<int[]> trace = new ArrayList<>();
        int limit = (int) Math.min(Math.min(n + m, MAX_EDITS), TRACE_BUDGET / v.length);
        for (int d = 0; d <= limit; d++) {
            trace.add(v.clone());
            for (int k = -d; k <= d; k += 2) {
                int x = k == -d || (k != d && v[offset + k - 1] < v[offset + k + 1])
                        ? v[offset + k + 1] : v[offset + k - 1] + 1;
                int y = x - k;
                while (x < n && y < m && a.get(x).equals(b.get(y))) {
                    x++;
                    y++;
                }
                v[offset + k] = x;
                if (x >= n && y >= m) {
                    backtrack(trace, offset, n, m, keptA, keptB);
                    return true;
                }
            }
        }
        return false;
    }

    private static void backtrack(List<int[]> trace, int offset, int n, int m, boolean[] keptA, boolean[] keptB) {
        // Восстанавливает совпавшие строки по истории шагов
        int x = n;
        int y = m;
        for (int d = trace.size() - 1; d > 0; d--) {
            int[] v = trace.get(d);
            int k = x - y;
            int previous = k == -d || (k != d && v[offset + k - 1] < v[offset + k + 1]) ? k + 1 : k - 1;
            int startX = v[offset + previous];
            int startY = startX - previous;
            while (x > startX && y > startY) {
                keptA[--x] = true;
                keptB[--y] = true;
            }
            x = startX;
            y = startY;
        }
        while (x > 0 && y > 0) {
            keptA[--x] = true;
            keptB[--y] = true;
        }
    }

    private static List<DiffHunk> hunks(List<String> a, boolean[] keptA, boolean[] keptB, int prefix) {
        // Собирает подряд идущие правки в блоки
        List<DiffHunk> result = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < keptA.length || j < keptB.length) {
            if (i < keptA.length && j < keptB.length && keptA[i] && keptB[j]) {
                i++;
                j++;
                continue;
            }
            int startA = i;
            int startB = j;
            while (i < keptA.length && !keptA[i]) {
                i++;
            }
            while (j < keptB.length && !keptB[j]) {
                j++;
            }
            result.add(new DiffHunk(prefix + startA, a.subList(startA, i), prefix + startB, j - startB));
        }
        return result;
    }
}
