package com.example.edtai.ui.context;

import com.example.edtai.domain.CodeProblem;
import com.example.edtai.domain.CodeProblem.Severity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotation;

final class EditorProblems {

    private static final int LIMIT = 40;

    private EditorProblems() {
    }

    static List<CodeProblem> collect(ITextEditor editor, int startLine, int endLine) {
        // Собирает ошибки и предупреждения редактора
        IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        IAnnotationModel model = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
        if (document == null || model == null) {
            return List.of();
        }
        Map<String, CodeProblem> found = new LinkedHashMap<>();
        for (Iterator<Annotation> it = model.getAnnotationIterator(); it.hasNext();) {
            Annotation annotation = it.next();
            Optional<Severity> severity = severity(annotation);
            Position position = model.getPosition(annotation);
            boolean usable = severity.isPresent() && position != null && !annotation.isMarkedDeleted();
            if (!usable || annotation.getText() == null) {
                continue;
            }
            int line = lineOf(document, position.getOffset());
            if (startLine > 0 && (line < startLine || line > endLine)) {
                continue;
            }
            CodeProblem problem = new CodeProblem(severity.get(), line, annotation.getText().strip());
            found.putIfAbsent(line + ":" + problem.message(), problem);
        }
        List<CodeProblem> result = new ArrayList<>(found.values());
        result.sort(Comparator.comparing(CodeProblem::severity).thenComparingInt(CodeProblem::line));
        return result.size() > LIMIT ? result.subList(0, LIMIT) : result;
    }

    private static Optional<Severity> severity(Annotation annotation) {
        // Определяет важность аннотации редактора
        if (annotation instanceof MarkerAnnotation marked && marked.getMarker().exists()) {
            int value = marked.getMarker().getAttribute(IMarker.SEVERITY, -1);
            if (value == IMarker.SEVERITY_ERROR) {
                return Optional.of(Severity.ERROR);
            }
            if (value == IMarker.SEVERITY_WARNING) {
                return Optional.of(Severity.WARNING);
            }
            if (value == IMarker.SEVERITY_INFO) {
                return Optional.of(Severity.INFO);
            }
        }
        String type = annotation.getType() == null ? "" : annotation.getType().toLowerCase(Locale.ROOT);
        if (type.endsWith(".error") || type.endsWith("error")) {
            return Optional.of(Severity.ERROR);
        }
        if (type.endsWith(".warning") || type.endsWith("warning")) {
            return Optional.of(Severity.WARNING);
        }
        return Optional.empty();
    }

    private static int lineOf(IDocument document, int offset) {
        // Переводит смещение в номер строки
        try {
            return document.getLineOfOffset(offset) + 1;
        } catch (BadLocationException e) {
            return 0;
        }
    }
}
