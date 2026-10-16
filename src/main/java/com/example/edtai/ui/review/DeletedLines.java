package com.example.edtai.ui.review;

import com.example.edtai.application.ReviewSignals;
import java.util.Map;
import java.util.WeakHashMap;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.jface.text.source.ISourceViewerExtension5;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

public final class DeletedLines {

    private static final int DELAY_MS = 600;
    private static final Map<ISourceViewerExtension5, Boolean> INSTALLED = new WeakHashMap<>();

    private DeletedLines() {
    }

    public static void install(Display display) {
        // Подключает показ удаленных строк к редакторам
        IPartListener2 listener = new IPartListener2() {
            @Override
            public void partOpened(IWorkbenchPartReference reference) {
                // Подключается к открытому редактору
                attach(reference.getPart(false));
            }
        };
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                page.addPartListener(listener);
                for (IEditorReference reference : page.getEditorReferences()) {
                    attach(reference.getEditor(false));
                }
            }
        }
        ReviewSignals.subscribe(files -> display.asyncExec(() -> display.timerExec(DELAY_MS, DeletedLines::refresh)));
    }

    private static void attach(Object part) {
        // Ставит поставщика в свободный редактор
        try {
            ITextEditor editor = part instanceof IEditorPart editorPart ? editorPart.getAdapter(ITextEditor.class)
                    : null;
            if (editor == null) {
                return;
            }
            Object target = editor.getAdapter(ITextOperationTarget.class);
            if (!(target instanceof ISourceViewerExtension5 viewer) || INSTALLED.containsKey(viewer)
                    || viewer.hasCodeMiningProviders()) {
                return;
            }
            viewer.setCodeMiningProviders(new ICodeMiningProvider[] {new DeletedLinesProvider(editor)});
            INSTALLED.put(viewer, Boolean.TRUE);
            IAnnotationModel model = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
            if (model != null) {
                IAnnotationModelListener update = changed -> Display.getDefault().asyncExec(() -> update(viewer));
                model.addAnnotationModelListener(update);
            }
            viewer.updateCodeMinings();
        } catch (RuntimeException | LinkageError ignored) {
            return;
        }
    }

    private static void refresh() {
        // Обновляет удаленные строки во всех редакторах
        for (ISourceViewerExtension5 viewer : INSTALLED.keySet().toArray(ISourceViewerExtension5[]::new)) {
            update(viewer);
        }
    }

    private static void update(ISourceViewerExtension5 viewer) {
        // Перерисовывает удаленные строки одного редактора
        try {
            viewer.updateCodeMinings();
        } catch (RuntimeException | LinkageError ignored) {
            return;
        }
    }
}
