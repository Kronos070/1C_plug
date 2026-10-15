package com.example.edtai.ui.docs;

import com.example.edtai.infrastructure.marks.WorkspaceText;
import com.example.edtai.infrastructure.settings.SettingsStore;
import com.example.edtai.infrastructure.settings.WorkspaceLocator;
import com.example.edtai.ui.feedback.CodeTargets;
import java.nio.file.Path;
import java.util.Optional;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

final class CodeOpener {

    private CodeOpener() {
    }

    static Optional<String> open(String path, int line) {
        // Открывает файл и переходит к строке
        if (path.isBlank()) {
            return Optional.of("У записи нет ссылки на код");
        }
        Optional<IFile> file = resolve(path);
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage page = window == null ? null : window.getActivePage();
        if (file.isEmpty() || page == null) {
            return Optional.of("Файл не найден в рабочей области: " + path);
        }
        try {
            IEditorPart editor = IDE.openEditor(page, file.get(), true);
            CodeTargets.textEditor(editor).ifPresent(text -> reveal(text, line));
            return Optional.empty();
        } catch (PartInitException e) {
            return Optional.of("Не удалось открыть файл: " + e.getMessage());
        }
    }

    private static Optional<IFile> resolve(String path) {
        // Ищет файл по абсолютному или относительному пути
        String normalized = path.replace('\\', '/');
        if (Path.of(normalized).isAbsolute()) {
            return WorkspaceText.fileFor(normalized);
        }
        Path root = WorkspaceLocator.resolve(new SettingsStore().load().workDirectory());
        Optional<IFile> fromRoot = WorkspaceText.fileFor(root.resolve(normalized).toString());
        if (fromRoot.isPresent() && fromRoot.get().exists()) {
            return fromRoot;
        }
        for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
            if (!project.isOpen()) {
                continue;
            }
            IFile direct = project.getFile(normalized);
            if (direct.exists()) {
                return Optional.of(direct);
            }
            String prefix = project.getName() + "/";
            if (normalized.startsWith(prefix) && project.getFile(normalized.substring(prefix.length())).exists()) {
                return Optional.of(project.getFile(normalized.substring(prefix.length())));
            }
        }
        return Optional.empty();
    }

    private static void reveal(ITextEditor editor, int line) {
        // Выделяет начало нужной строки
        IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        if (document == null || line <= 0) {
            return;
        }
        try {
            IRegion region = document.getLineInformation(Math.min(line, document.getNumberOfLines()) - 1);
            editor.selectAndReveal(region.getOffset(), 0);
        } catch (BadLocationException ignored) {
            editor.selectAndReveal(0, 0);
        }
    }
}
