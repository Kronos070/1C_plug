package com.example.edtai.ui.marks;

import com.example.edtai.infrastructure.marks.AiMarkers;
import com.example.edtai.infrastructure.marks.ReviewMarkers;
import com.example.edtai.infrastructure.marks.WorkspaceText;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

public final class AnnotationSync {

    private static final int ATTEMPTS = 12;
    private static final int DELAY_MS = 500;
    private static final String SYNCED = "synced";

    private AnnotationSync() {
    }

    public static void refresh(IFile file) {
        // Показывает новые метки в открытых редакторах
        Display display = PlatformUI.getWorkbench().getDisplay();
        display.asyncExec(() -> attempt(file, ATTEMPTS));
    }

    private static void attempt(IFile file, int left) {
        // Ждет, пока редактор подхватит новый текст
        String text;
        try {
            text = WorkspaceText.read(file);
        } catch (CoreException | IOException e) {
            return;
        }
        boolean stale = false;
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IEditorReference reference : page.getEditorReferences()) {
                    stale |= isStale(reference, file, text);
                }
            }
        }
        if (stale && left > 0) {
            Display.getCurrent().timerExec(DELAY_MS, () -> attempt(file, left - 1));
            return;
        }
        List<IMarker> markers = new ArrayList<>(AiMarkers.find(file));
        for (String type : List.of(ReviewMarkers.ADDED, ReviewMarkers.MODIFIED, ReviewMarkers.DELETED)) {
            markers.addAll(ReviewMarkers.find(file, type));
        }
        for (IMarker marker : markers) {
            try {
                marker.setAttribute(SYNCED, Long.toString(System.currentTimeMillis()));
            } catch (CoreException ignored) {
                return;
            }
        }
    }

    private static boolean isStale(IEditorReference reference, IFile file, String text) {
        // Проверяет, что редактор показывает старый текст
        try {
            if (!file.equals(reference.getEditorInput().getAdapter(IFile.class))) {
                return false;
            }
        } catch (PartInitException e) {
            return false;
        }
        ITextEditor editor = reference.getEditor(false) == null ? null
                : reference.getEditor(false).getAdapter(ITextEditor.class);
        IDocument document = editor == null ? null : editor.getDocumentProvider().getDocument(editor.getEditorInput());
        return document != null && !editor.isDirty() && !document.get().equals(text);
    }
}
