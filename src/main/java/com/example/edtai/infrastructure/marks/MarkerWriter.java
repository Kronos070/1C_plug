package com.example.edtai.infrastructure.marks;

import com.example.edtai.application.port.AiCodeMarks;
import com.example.edtai.domain.AiProvenance;
import com.example.edtai.domain.LineRange;
import java.io.IOException;
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

public final class MarkerWriter implements AiCodeMarks {

    private final Consumer<IFile> afterMark;

    public MarkerWriter(Consumer<IFile> afterMark) {
        this.afterMark = afterMark;
    }

    @Override
    public void markWritten(String path, List<String> snippets, List<LineRange> ranges, AiProvenance provenance) {
        // Ставит метки на код, записанный агентом
        Optional<IFile> file = WorkspaceText.fileFor(path);
        if (file.isEmpty()) {
            return;
        }
        WorkspaceJob job = new WorkspaceJob("Метки кода Перуна") {
            @Override
            public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
                // Обновляет файл и ставит метки
                file.get().getParent().refreshLocal(IResource.DEPTH_ONE, monitor);
                if (!file.get().exists()) {
                    return Status.OK_STATUS;
                }
                try {
                    String text = WorkspaceText.read(file.get());
                    if (ranges != null && !ranges.isEmpty()) {
                        markRanges(file.get(), text, ranges, provenance);
                    } else {
                        mark(file.get(), text, snippets, provenance);
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
        job.setRule(MultiRule.combine(rules.refreshRule(file.get().getParent()), rules.markerRule(file.get())));
        job.schedule(300);
    }

    private static void markRanges(IFile file, String text, List<LineRange> ranges, AiProvenance provenance)
            throws CoreException {
        // Отмечает диапазоны строк напрямую по смещениям
        List<int[]> lines = TextLines.offsets(text);
        if (lines.isEmpty()) {
            return;
        }
        int len = text.length();
        for (LineRange range : ranges) {
            int startLine = range.startLine();
            int endLine = range.endLine();
            if (startLine < 1 || startLine > lines.size() || endLine < startLine) {
                continue;
            }
            int effectiveEnd = Math.min(endLine, lines.size());
            int start = Math.max(0, Math.min(lines.get(startLine - 1)[0], len));
            int end = Math.max(start, Math.min(lines.get(effectiveEnd - 1)[1], len));
            if (!covered(file, start, end)) {
                AiMarkers.create(file, start, end, text.substring(start, end), provenance);
            }
        }
    }

    private static void mark(IFile file, String text, List<String> snippets, AiProvenance provenance)
            throws CoreException {
        // Находит фрагменты в тексте и отмечает их
        for (String snippet : snippets) {
            String code = snippet.strip();
            if (code.isEmpty()) {
                continue;
            }
            int start = text.indexOf(code);
            if (start < 0) {
                start = text.indexOf(code.replace("\n", "\r\n"));
                code = code.replace("\n", "\r\n");
            }
            if (start >= 0 && !covered(file, start, start + code.length())) {
                AiMarkers.create(file, start, start + code.length(), code, provenance);
            }
        }
    }

    private static boolean covered(IFile file, int start, int end) {
        // Проверяет, отмечен ли уже этот диапазон
        for (IMarker marker : AiMarkers.find(file)) {
            if (marker.getAttribute(IMarker.CHAR_START, -1) == start
                    && marker.getAttribute(IMarker.CHAR_END, -1) == end) {
                return true;
            }
        }
        return false;
    }
}
