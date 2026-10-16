package com.example.edtai.ui.docs;

import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

public final class DocsOpener {

    private DocsOpener() {
    }

    public static void open(String id) {
        // Открывает вкладку документации на записи
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage page = window == null ? null : window.getActivePage();
        if (page == null) {
            return;
        }
        try {
            IEditorPart part = page.openEditor(DocsEditorInput.INSTANCE, DocsEditor.ID, true);
            if (part instanceof DocsEditor docs && id != null && !id.isBlank()) {
                docs.show(id);
            }
        } catch (PartInitException e) {
            StatusManager.getManager().handle(Status.error("Не удалось открыть документацию", e),
                    StatusManager.LOG | StatusManager.SHOW);
        }
    }
}
