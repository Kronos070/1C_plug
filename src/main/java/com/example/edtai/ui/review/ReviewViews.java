package com.example.edtai.ui.review;

import com.example.edtai.domain.ChangeKind;
import com.example.edtai.domain.DiffHunk;
import com.example.edtai.domain.ReviewFile;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.List;

public final class ReviewViews {

    private static final int PREVIEW_LINES = 8;
    private static final int PREVIEW_HUNKS = 6;

    private ReviewViews() {
    }

    public static JsonObject summary(List<ReviewFile> files) {
        // Переводит непринятые правки в JSON страницы
        JsonArray items = new JsonArray();
        for (ReviewFile file : files) {
            items.add(file(file));
        }
        JsonObject json = new JsonObject();
        json.addProperty("type", "review");
        json.add("files", items);
        return json;
    }

    private static JsonObject file(ReviewFile file) {
        // Описывает правки одного файла
        Path path = Path.of(file.path());
        JsonObject json = new JsonObject();
        json.addProperty("path", file.path());
        json.addProperty("name", path.getFileName() == null ? file.path() : path.getFileName().toString());
        json.addProperty("folder", path.getParent() == null || path.getParent().getFileName() == null ? ""
                : path.getParent().getFileName().toString());
        json.addProperty("added", file.count(ChangeKind.ADDED));
        json.addProperty("modified", file.count(ChangeKind.MODIFIED));
        json.addProperty("deleted", file.count(ChangeKind.DELETED));
        json.addProperty("revertible", file.revertible());
        json.addProperty("line", file.hunks().isEmpty() ? 1 : file.hunks().get(0).newStart() + 1);
        JsonArray hunks = new JsonArray();
        file.hunks().stream().limit(PREVIEW_HUNKS).forEach(hunk -> hunks.add(hunk(hunk, file.lines())));
        json.add("hunks", hunks);
        json.addProperty("more", Math.max(0, file.hunks().size() - PREVIEW_HUNKS));
        return json;
    }

    private static JsonObject hunk(DiffHunk hunk, List<String> lines) {
        // Описывает блок правок для предпросмотра
        JsonObject json = new JsonObject();
        json.addProperty("kind", hunk.kind().id());
        json.addProperty("line", hunk.newStart() + 1);
        json.add("old", preview(hunk.oldLines()));
        int end = Math.min(lines.size(), hunk.newStart() + hunk.newCount());
        json.add("new", preview(lines.subList(Math.min(hunk.newStart(), end), end)));
        json.addProperty("oldMore", Math.max(0, hunk.oldLines().size() - PREVIEW_LINES));
        json.addProperty("newMore", Math.max(0, hunk.newCount() - PREVIEW_LINES));
        return json;
    }

    private static JsonArray preview(List<String> lines) {
        // Берет первые строки блока
        JsonArray array = new JsonArray();
        lines.stream().limit(PREVIEW_LINES).forEach(array::add);
        return array;
    }
}
