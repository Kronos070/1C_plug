package com.example.edtai.ui.docs;

import com.example.edtai.bootstrap.DocsAssembler;
import com.example.edtai.domain.BslSymbolLocator;
import com.example.edtai.ui.feedback.CodeTargets;
import java.nio.file.Path;
import java.util.Optional;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

public final class OpenDocsHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) {
        // Открывает документацию метода под курсором
        Optional<ITextEditor> found = CodeTargets.textEditor(HandlerUtil.getActiveEditor(event));
        if (found.isEmpty()) {
            return null;
        }
        ITextEditor editor = found.get();
        IFile file = editor.getEditorInput().getAdapter(IFile.class);
        IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        if (file == null || document == null) {
            return null;
        }
        String module = modulePath(file);
        Optional<String> symbol = symbol(editor, document);
        DocsOpener.open(symbol.map(name -> module + "#" + name).orElse(module));
        return null;
    }

    private static Optional<String> symbol(ITextEditor editor, IDocument document) {
        // Определяет метод под курсором
        if (!(editor.getSelectionProvider().getSelection() instanceof ITextSelection selection)) {
            return Optional.empty();
        }
        try {
            int line = document.getLineOfOffset(selection.getOffset());
            return BslSymbolLocator.symbolAt(document.get().split("\\R", -1), line);
        } catch (BadLocationException e) {
            return Optional.empty();
        }
    }

    private static String modulePath(IFile file) {
        // Строит путь модуля от каталога агента
        Path root = DocsAssembler.workDirectory();
        if (file.getLocation() != null && !root.toString().isEmpty()) {
            Path location = file.getLocation().toFile().toPath();
            if (location.startsWith(root)) {
                return root.relativize(location).toString().replace('\\', '/');
            }
        }
        return file.getProjectRelativePath().toString();
    }
}
