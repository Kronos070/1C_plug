package com.example.edtai.ui.feedback;

import java.util.Optional;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.texteditor.ITextEditor;

public record CodeTarget(
        ITextEditor editor,
        IFile file,
        int offset,
        int startLine,
        int endLine,
        String code,
        Optional<IMarker> marker) {

    public String path() {
        // Возвращает путь файла в рабочей области
        return file == null ? editor.getTitle() : file.getFullPath().toString();
    }

    public String relativePath() {
        // Возвращает путь файла внутри проекта
        return file == null ? editor.getTitle() : file.getProjectRelativePath().toString();
    }
}
