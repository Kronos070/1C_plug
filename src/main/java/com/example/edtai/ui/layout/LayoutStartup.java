package com.example.edtai.ui.layout;

import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PerspectiveAdapter;
import org.eclipse.ui.PlatformUI;

public final class LayoutStartup implements IStartup {

    @Override
    public void earlyStartup() {
        // Готовит место чата после запуска EDT
        IWorkbench workbench = PlatformUI.getWorkbench();
        workbench.getDisplay().asyncExec(() -> {
            for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
                watch(window);
            }
            workbench.addWindowListener(new WindowWatcher());
        });
    }

    private static void watch(IWorkbenchWindow window) {
        // Следит за перспективами окна
        prepare(window, false);
        window.addPerspectiveListener(new PerspectiveAdapter() {
            @Override
            public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
                // Готовит место при смене перспективы
                prepare(window, false);
            }

            @Override
            public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String change) {
                // Готовит место после сброса перспективы
                if (IWorkbenchPage.CHANGE_RESET_COMPLETE.equals(change)) {
                    prepare(window, true);
                }
            }
        });
    }

    private static void prepare(IWorkbenchWindow window, boolean again) {
        // Применяет размещение к активной перспективе
        EModelService models = window.getService(EModelService.class);
        EPartService parts = window.getService(EPartService.class);
        MWindow model = window.getService(MWindow.class);
        if (models == null || parts == null || model == null) {
            return;
        }
        MPerspective perspective = models.getActivePerspective(model);
        if (perspective != null) {
            new ChatSlot(models, perspective).prepare(parts, again);
        }
    }

    private static final class WindowWatcher implements IWindowListener {

        @Override
        public void windowOpened(IWorkbenchWindow window) {
            // Подключает новое окно EDT
            watch(window);
        }

        @Override
        public void windowActivated(IWorkbenchWindow window) {
            // Не требует действий
        }

        @Override
        public void windowDeactivated(IWorkbenchWindow window) {
            // Не требует действий
        }

        @Override
        public void windowClosed(IWorkbenchWindow window) {
            // Не требует действий
        }
    }
}
