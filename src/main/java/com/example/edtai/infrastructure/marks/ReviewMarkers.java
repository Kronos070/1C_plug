package com.example.edtai.infrastructure.marks;

import com.example.edtai.application.port.ReviewMarks;
import com.example.edtai.domain.ChangeKind;
import com.example.edtai.domain.DiffHunk;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.MultiRule;

public final class ReviewMarkers implements ReviewMarks {

    public static final String ADDED = "com.example.edtai.aiAdded";
    public static final String MODIFIED = "com.example.edtai.aiModified";
    public static final String DELETED = "com.example.edtai.aiDeleted";
    public static final String OLD_TEXT = "oldText";

    private static final int MESSAGE_LIMIT = 1500;

    private final Consumer<IFile> afterMark;

    public ReviewMarkers(Consumer<IFile> afterMark) {
        this.afterMark = afterMark;
    }

    @Override
    public void show(String path, List<DiffHunk> hunks) {
        // Подсвечивает правки агента в файле
        Optional<IFile> file = WorkspaceText.fileFor(path);
        if (file.isEmpty()) {
            return;
        }
        WorkspaceJob job = new WorkspaceJob("Правки Перуна") {
            @Override
            public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
                // Пересоздает метки правок по тексту
                file.get().refreshLocal(IResource.DEPTH_ZERO, monitor);
                if (!file.get().exists()) {
                    return Status.OK_STATUS;
                }
                try {
                    List<int[]> lines = TextLines.offsets(WorkspaceText.read(file.get()));
                    delete(file.get());
                    for (DiffHunk hunk : hunks) {
                        markHunk(file.get(), lines, hunk);
                    }
                    afterMark.accept(file.get());
                } catch (IOException e) {
                    return Status.warning(e.getMessage());
                }
                return Status.OK_STATUS;
            }
        };
        job.setSystem(true);
        IResourceRuleFactory rules = ResourcesPlugin.getWorkspace().getRuleFactory();
        job.setRule(MultiRule.combine(rules.refreshRule(file.get()), rules.markerRule(file.get())));
        job.schedule(300);
    }

    @Override
    public void clear(String path, boolean dropAiCode) {
        // Убирает подсветку правок и при откате код ИИ
        Optional<IFile> file = WorkspaceText.fileFor(path);
        if (file.isEmpty() || !file.get().exists()) {
            return;
        }
        try {
            if (dropAiCode) {
                List<IMarker> changes = find(file.get(), ADDED);
                changes.addAll(find(file.get(), MODIFIED));
                for (IMarker code : AiMarkers.find(file.get())) {
                    if (changes.stream().anyMatch(change -> overlaps(change, code))) {
                        code.delete();
                    }
                }
            }
            delete(file.get());
        } catch (CoreException ignored) {
            return;
        }
    }

    public static List<IMarker> find(IFile file, String type) {
        // Возвращает метки правок одного вида
        try {
            return file.exists() ? new ArrayList<>(List.of(file.findMarkers(type, false, IResource.DEPTH_ZERO)))
                    : new ArrayList<>();
        } catch (CoreException e) {
            return new ArrayList<>();
        }
    }

    private static void markHunk(IFile file, List<int[]> lines, DiffHunk hunk) throws CoreException {
        // Создает метку для одного блока правок
        ChangeKind kind = hunk.kind();
        String old = String.join("\n", hunk.oldLines());
        int first = Math.min(hunk.newStart(), lines.size() - 1);
        int start = hunk.newStart() < lines.size() ? lines.get(first)[0] : lines.get(lines.size() - 1)[1];
        int end = kind == ChangeKind.DELETED ? start
                : lines.get(Math.min(hunk.newStart() + hunk.newCount() - 1, lines.size() - 1))[1];
        IMarker marker = file.createMarker(kind == ChangeKind.ADDED ? ADDED : kind == ChangeKind.MODIFIED ? MODIFIED
                : DELETED);
        marker.setAttributes(new String[] {IMarker.CHAR_START, IMarker.CHAR_END, IMarker.LINE_NUMBER,
            IMarker.MESSAGE, OLD_TEXT}, new Object[] {start, end, first + 1, message(kind, hunk, old),
            limit(old)});
    }

    private static String message(ChangeKind kind, DiffHunk hunk, String old) {
        // Формирует подсказку к метке правки
        return switch (kind) {
            case ADDED -> "Добавлено Перуном · примите или отмените правки в чате";
            case MODIFIED -> "Изменено Перуном. Было:\n" + limit(old);
            case DELETED -> "Удалено Перуном (" + hunk.oldLines().size() + " стр.):\n" + limit(old);
        };
    }

    private static boolean overlaps(IMarker change, IMarker code) {
        // Проверяет пересечение двух меток
        int start = change.getAttribute(IMarker.CHAR_START, -1);
        int end = change.getAttribute(IMarker.CHAR_END, -1);
        int codeStart = code.getAttribute(IMarker.CHAR_START, -1);
        int codeEnd = code.getAttribute(IMarker.CHAR_END, -1);
        return start >= 0 && codeStart >= 0 && codeStart < end && start < codeEnd;
    }

    private static void delete(IFile file) throws CoreException {
        // Удаляет все метки правок в файле
        for (String type : List.of(ADDED, MODIFIED, DELETED)) {
            file.deleteMarkers(type, false, IResource.DEPTH_ZERO);
        }
    }

    private static String limit(String text) {
        // Обрезает длинный текст подсказки
        return text.length() <= MESSAGE_LIMIT ? text : text.substring(0, MESSAGE_LIMIT) + "\n…";
    }
}
