package com.example.edtai.ui.feedback;

import com.example.edtai.domain.AiProvenance;
import com.example.edtai.infrastructure.marks.AiMarkers;
import java.util.Optional;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

public final class EditorInserter {

    private EditorInserter() {
    }

    public static Optional<String> insert(String code, AiProvenance provenance) {
        // Вставляет код ИИ в активный редактор
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage page = window == null ? null : window.getActivePage();
        Optional<ITextEditor> found = CodeTargets.textEditor(page == null ? null : page.getActiveEditor());
        if (found.isEmpty()) {
            return Optional.of("Откройте модуль в редакторе, чтобы вставить код");
        }
        ITextEditor editor = found.get();
        IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        if (document == null || !(editor.getSelectionProvider().getSelection() instanceof ITextSelection selection)) {
            return Optional.of("Не удалось определить позицию вставки");
        }
        String text = code.replace("\r\n", "\n").replace("\n", TextUtilities.getDefaultLineDelimiter(document));
        try {
            document.replace(selection.getOffset(), selection.getLength(), text);
            editor.selectAndReveal(selection.getOffset(), text.length());
            IFile file = editor.getEditorInput().getAdapter(IFile.class);
            if (file != null) {
                AiMarkers.create(file, selection.getOffset(), selection.getOffset() + text.length(), text,
                        provenance);
            }
            page.activate(editor);
            return Optional.empty();
        } catch (BadLocationException | CoreException e) {
            return Optional.of("Не удалось вставить код: " + e.getMessage());
        }
    }
}
