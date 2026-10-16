package com.example.edtai.ui.review;

import com.example.edtai.infrastructure.marks.ReviewMarkers;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.codemining.AbstractCodeMiningProvider;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.jface.text.codemining.LineHeaderCodeMining;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotation;

final class DeletedLinesProvider extends AbstractCodeMiningProvider {

    private static final RGB RED = new RGB(214, 72, 64);
    private static final int LINE_LIMIT = 30;

    private final ITextEditor editor;

    DeletedLinesProvider(ITextEditor editor) {
        this.editor = editor;
    }

    @Override
    public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(ITextViewer viewer,
            IProgressMonitor monitor) {
        // Показывает удаленные строки над местом удаления
        List<ICodeMining> minings = new ArrayList<>();
        IAnnotationModel model = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
        IDocument document = viewer.getDocument();
        if (model == null || document == null) {
            return CompletableFuture.completedFuture(minings);
        }
        Iterator<Annotation> annotations = model.getAnnotationIterator();
        while (annotations.hasNext()) {
            Annotation annotation = annotations.next();
            Position position = model.getPosition(annotation);
            if (annotation.isMarkedDeleted() || position == null || !(annotation instanceof MarkerAnnotation marker)
                    || !ReviewMarkers.DELETED.equals(annotation.getType())) {
                continue;
            }
            try {
                String text = marker.getMarker().getAttribute(ReviewMarkers.OLD_TEXT, "");
                minings.add(new DeletedLine(document.getLineOfOffset(position.getOffset()), document, this, text));
            } catch (BadLocationException ignored) {
                continue;
            }
        }
        return CompletableFuture.completedFuture(minings);
    }

    private static final class DeletedLine extends LineHeaderCodeMining {

        DeletedLine(int line, IDocument document, DeletedLinesProvider provider, String text)
                throws BadLocationException {
            super(line, document, provider);
            List<String> lines = text.lines().limit(LINE_LIMIT).map(value -> "− " + value.replace("\t", "    "))
                    .toList();
            setLabel(String.join("\n", lines.isEmpty() ? List.of("− ") : lines));
        }

        @Override
        public Point draw(GC gc, StyledText widget, Color color, int x, int y) {
            // Рисует удаленный код красным и зачеркнутым
            Color red = new Color(RED);
            gc.setForeground(red);
            Point size = super.draw(gc, widget, red, x, y);
            String[] lines = getLabel().split("\n");
            int height = lines.length == 0 ? 0 : size.y / lines.length;
            for (int i = 0; i < lines.length; i++) {
                int middle = y + i * height + height / 2;
                gc.drawLine(x, middle, x + gc.stringExtent(lines[i]).x, middle);
            }
            return size;
        }
    }
}
