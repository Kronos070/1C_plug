package com.example.edtai.infrastructure.settings;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public final class WorkspaceLocator {

    private WorkspaceLocator() {
    }

    public static Path resolve(String configured) {
        // 1. Явно заданный каталог в настройках
        if (configured != null && !configured.isBlank() && Files.isDirectory(Path.of(configured))) {
            return Path.of(configured);
        }

        // 2. Проект из открытого редактора
        IProject activeProject = getActiveEditorProject();
        if (isValid(activeProject)) {
            return activeProject.getLocation().toFile().toPath();
        }

        // 3. Проект из выделения в интерфейсе
        IProject selectedProject = getSelectedProject();
        if (isValid(selectedProject)) {
            return selectedProject.getLocation().toFile().toPath();
        }

        // 4. Поиск наилучшего открытого проекта 1С в рабочей области
        IProject bestProject = findBestProject();
        if (isValid(bestProject)) {
            return bestProject.getLocation().toFile().toPath();
        }

        // 5. Каталог рабочей области Eclipse (fallback)
        try {
            IPath location = ResourcesPlugin.getWorkspace().getRoot().getLocation();
            if (location != null && location.toFile().isDirectory()) {
                return location.toFile().toPath();
            }
        } catch (Exception ignored) {
        }

        return Path.of(System.getProperty("user.home"));
    }

    private static boolean isValid(IProject project) {
        return project != null && project.isOpen() && project.getLocation() != null;
    }

    private static IProject getActiveEditorProject() {
        if (!PlatformUI.isWorkbenchRunning()) {
            return null;
        }
        try {
            Display display = PlatformUI.getWorkbench().getDisplay();
            if (display == null || display.isDisposed()) {
                return null;
            }
            if (Thread.currentThread() == display.getThread()) {
                return extractEditorProject();
            }
            final IProject[] result = new IProject[1];
            display.syncExec(() -> result[0] = extractEditorProject());
            return result[0];
        } catch (Exception ignored) {
            return null;
        }
    }

    private static IProject extractEditorProject() {
        try {
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (window == null) {
                IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
                if (windows.length > 0) {
                    window = windows[0];
                }
            }
            if (window != null) {
                IWorkbenchPage page = window.getActivePage();
                if (page != null) {
                    IEditorPart editor = page.getActiveEditor();
                    if (editor != null && editor.getEditorInput() != null) {
                        IEditorInput input = editor.getEditorInput();
                        IResource resource = input.getAdapter(IResource.class);
                        if (resource != null) {
                            return resource.getProject();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static IProject getSelectedProject() {
        if (!PlatformUI.isWorkbenchRunning()) {
            return null;
        }
        try {
            Display display = PlatformUI.getWorkbench().getDisplay();
            if (display == null || display.isDisposed()) {
                return null;
            }
            if (Thread.currentThread() == display.getThread()) {
                return extractSelectedProject();
            }
            final IProject[] result = new IProject[1];
            display.syncExec(() -> result[0] = extractSelectedProject());
            return result[0];
        } catch (Exception ignored) {
            return null;
        }
    }

    private static IProject extractSelectedProject() {
        try {
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (window != null && window.getSelectionService() != null) {
                ISelection selection = window.getSelectionService().getSelection();
                if (selection instanceof IStructuredSelection structured && !structured.isEmpty()) {
                    Object element = structured.getFirstElement();
                    if (element instanceof IResource resource) {
                        return resource.getProject();
                    }
                    if (element instanceof IAdaptable adaptable) {
                        IResource resource = adaptable.getAdapter(IResource.class);
                        if (resource != null) {
                            return resource.getProject();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static IProject findBestProject() {
        try {
            IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
            if (projects == null || projects.length == 0) {
                return null;
            }
            IProject fallback = null;
            for (IProject project : projects) {
                if (!isValid(project)) {
                    continue;
                }
                if (is1cProject(project)) {
                    if (!project.getName().contains(".")) {
                        return project;
                    }
                    if (fallback == null) {
                        fallback = project;
                    }
                }
            }
            if (fallback != null) {
                return fallback;
            }
            for (IProject project : projects) {
                if (isValid(project)) {
                    return project;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static boolean is1cProject(IProject project) {
        try {
            if (project.hasNature("com._1c.g5.v8.dt.core.V8ConfigurationNature")
                    || project.hasNature("com._1c.g5.v8.dt.core.V8ExtensionNature")
                    || project.hasNature("com._1c.g5.v8.dt.core.v8ProjectNature")) {
                return true;
            }
        } catch (Exception ignored) {
        }
        IPath loc = project.getLocation();
        if (loc == null) {
            return false;
        }
        File dir = loc.toFile();
        return new File(dir, "DT-INF/PROJECT.PMF").exists()
                || new File(dir, "src/Configuration/Configuration.mdo").exists()
                || new File(dir, "src/Configuration.mdo").exists()
                || new File(dir, "Configuration.mdo").exists()
                || new File(dir, "dt-project.json").exists();
    }
}
