package com.example.edtai.ui.context;

import com.example.edtai.domain.CodeContext;
import java.util.Optional;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

public final class EditorContextReader {

    private EditorContextReader() {
    }

    public static Optional<CodeContext> read(IEditorPart editor) {
        // Читает контекст из текущего выделения
        ISelectionProvider provider = editor.getSite().getSelectionProvider();
        return read(editor, provider == null ? null : provider.getSelection());
    }

    public static Optional<CodeContext> read(IEditorPart editor, ISelection selection) {
        // Извлекает выделенный код или файл
        Optional<String> path = path(nested(editor).getEditorInput());
        if (path.isEmpty()) {
            return Optional.empty();
        }
        CodeContext context = CodeContext.file(path.get());
        if (selection instanceof ITextSelection text && text.getStartLine() >= 0 && text.getText() != null
                && !text.getText().isBlank()) {
            String code = text.getText().replace("\r\n", "\n");
            context = CodeContext.selection(path.get(), text.getStartLine() + 1, text.getEndLine() + 1, code);
        }
        ITextEditor textEditor = nested(editor).getAdapter(ITextEditor.class);
        return Optional.of(textEditor == null ? context
                : context.withProblems(EditorProblems.collect(textEditor, context.startLine(), context.endLine())));
    }

    private static IEditorPart nested(IEditorPart editor) {
        // Находит активную вкладку составного редактора
        if (editor instanceof MultiPageEditorPart multi && multi.getSelectedPage() instanceof IEditorPart page) {
            return page;
        }
        return editor;
    }

    private static Optional<String> path(IEditorInput input) {
        // Находит путь файла редактора на диске
        if (input == null) {
            return Optional.empty();
        }
        IFile file = input.getAdapter(IFile.class);
        if (file != null && file.getLocation() != null) {
            return Optional.of(file.getLocation().toOSString());
        }
        if (input instanceof IPathEditorInput pathInput) {
            return Optional.of(pathInput.getPath().toOSString());
        }
        return Optional.empty();
    }
}
