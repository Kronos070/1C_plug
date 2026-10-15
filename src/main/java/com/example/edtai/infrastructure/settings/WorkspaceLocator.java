package com.example.edtai.infrastructure.settings;

import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

public final class WorkspaceLocator {

    private WorkspaceLocator() {
    }

    public static Path resolve(String configured) {
        // Определяет рабочий каталог агента
        if (configured != null && !configured.isBlank() && Files.isDirectory(Path.of(configured))) {
            return Path.of(configured);
        }
        IPath location = ResourcesPlugin.getWorkspace().getRoot().getLocation();
        if (location != null && location.toFile().isDirectory()) {
            return location.toFile().toPath();
        }
        return Path.of(System.getProperty("user.home"));
    }
}
