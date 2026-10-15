package com.example.edtai.infrastructure.marks;

import com.example.edtai.application.FeedbackService;
import com.example.edtai.domain.AiProvenance;
import com.example.edtai.domain.Feedback;
import com.example.edtai.domain.FeedbackKind;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public final class RewriteWatcher implements IResourceChangeListener {

    private static final double THRESHOLD = 0.4;

    private final FeedbackService feedback;

    public RewriteWatcher(FeedbackService feedback) {
        this.feedback = feedback;
    }

    public void install() {
        // Подписывается на изменения рабочей области
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        // Отслеживает правки и удаление кода ИИ
        Set<IFile> changed = new HashSet<>();
        List<IMarkerDelta> removed = new ArrayList<>();
        for (IMarkerDelta delta : event.findMarkerDeltas(AiMarkers.TYPE, false)) {
            if (delta.getKind() == IResourceDelta.REMOVED) {
                removed.add(delta);
            } else if (delta.getResource() instanceof IFile file) {
                changed.add(file);
            }
        }
        try {
            event.getDelta().accept(delta -> {
                if (delta.getResource() instanceof IFile file && (delta.getFlags() & IResourceDelta.CONTENT) != 0
                        && !AiMarkers.find(file).isEmpty()) {
                    changed.add(file);
                }
                return true;
            });
        } catch (CoreException ignored) {
            return;
        }
        if (!changed.isEmpty() || !removed.isEmpty()) {
            schedule(changed, removed);
        }
    }

    private void schedule(Set<IFile> changed, List<IMarkerDelta> removed) {
        // Запускает проверку вне потока событий
        WorkspaceJob job = new WorkspaceJob("Проверка кода Перуна") {
            @Override
            public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
                // Сохраняет неявные отзывы
                removed.forEach(RewriteWatcher.this::onRemoved);
                for (IFile file : changed) {
                    inspect(file);
                }
                return Status.OK_STATUS;
            }
        };
        job.setSystem(true);
        job.schedule(200);
    }

    private void onRemoved(IMarkerDelta delta) {
        // Записывает удаление блока ИИ
        if (!delta.getResource().exists() || delta.getAttribute(AiMarkers.REWRITTEN, false)) {
            return;
        }
        AiProvenance provenance = new AiProvenance(delta.getAttribute(AiMarkers.MODEL, ""),
                delta.getAttribute(AiMarkers.PROMPT, ""), delta.getAttribute(AiMarkers.SOURCE, ""));
        save(new Feedback(FeedbackKind.DELETED, "code", delta.getResource().getFullPath().toString(), 0, 0,
                List.of(), "", delta.getAttribute(AiMarkers.SNAPSHOT, ""), "", provenance,
                delta.getAttribute(AiMarkers.FRAGMENT, "")));
    }

    private void inspect(IFile file) throws CoreException {
        // Ищет сильно переписанные блоки ИИ
        String text;
        try {
            text = WorkspaceText.read(file);
        } catch (IOException e) {
            return;
        }
        for (IMarker marker : AiMarkers.find(file)) {
            if (marker.getAttribute(AiMarkers.REWRITTEN, false)) {
                continue;
            }
            int start = marker.getAttribute(IMarker.CHAR_START, 0);
            String current = WorkspaceText.slice(text, start, marker.getAttribute(IMarker.CHAR_END, 0));
            String original = marker.getAttribute(AiMarkers.SNAPSHOT, "");
            if (TextChange.ratio(original, current) > THRESHOLD) {
                marker.setAttribute(AiMarkers.REWRITTEN, true);
                int line = (int) text.substring(0, Math.min(start, text.length())).chars().filter(c -> c == '\n')
                        .count() + 1;
                save(new Feedback(FeedbackKind.REWRITTEN, "code", file.getFullPath().toString(), line,
                        line + (int) current.chars().filter(c -> c == '\n').count(), List.of(), "", original,
                        current, AiMarkers.provenance(marker), marker.getAttribute(AiMarkers.FRAGMENT, "")));
            }
        }
    }

    private void save(Feedback entry) {
        // Сохраняет отзыв, не прерывая работу
        try {
            feedback.submit(entry);
        } catch (IOException ignored) {
            return;
        }
    }
}
