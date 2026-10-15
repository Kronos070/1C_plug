package com.example.edtai.ui.feedback;

import com.example.edtai.infrastructure.marks.AiMarkers;
import java.util.Optional;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

public final class CodeTargets {

    private CodeTargets() {
    }

    public static Optional<ITextEditor> textEditor(IEditorPart part) {
        // Находит текстовый редактор внутри части
        if (part instanceof MultiPageEditorPart multi && multi.getSelectedPage() instanceof IEditorPart page) {
            return textEditor(page);
        }
        return Optional.ofNullable(part == null ? null : part.getAdapter(ITextEditor.class));
    }

    public static Optional<CodeTarget> resolve(ITextEditor editor, boolean fromRuler) {
        // Определяет код для отзыва
        IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        IFile file = editor.getEditorInput().getAdapter(IFile.class);
        if (document == null) {
            return Optional.empty();
        }
        try {
            int start;
            int end;
            if (fromRuler) {
                IVerticalRulerInfo ruler = editor.getAdapter(IVerticalRulerInfo.class);
                int line = ruler == null ? -1 : ruler.getLineOfLastMouseButtonActivity();
                if (line < 0) {
                    return Optional.empty();
                }
                start = document.getLineOffset(line);
                end = start + document.getLineLength(line);
            } else {
                ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
                start = selection.getOffset();
                end = start + selection.getLength();
            }
            Optional<IMarker> marker = file == null ? Optional.empty() : markerAt(file, start, end);
            if (marker.isPresent() && (fromRuler || end == start)) {
                start = marker.get().getAttribute(IMarker.CHAR_START, start);
                end = Math.min(document.getLength(), marker.get().getAttribute(IMarker.CHAR_END, end));
            }
            int endLine = document.getLineOfOffset(Math.max(start, end - 1)) + 1;
            return Optional.of(new CodeTarget(editor, file, start, document.getLineOfOffset(start) + 1, endLine,
                    document.get(start, end - start), marker));
        } catch (BadLocationException | ClassCastException e) {
            return Optional.empty();
        }
    }

    private static Optional<IMarker> markerAt(IFile file, int start, int end) {
        // Находит метку ИИ, пересекающую диапазон
        return AiMarkers.find(file).stream()
                .filter(marker -> marker.getAttribute(IMarker.CHAR_START, -1) <= Math.max(start, end - 1)
                        && marker.getAttribute(IMarker.CHAR_END, -1) >= start)
                .findFirst();
    }
}
