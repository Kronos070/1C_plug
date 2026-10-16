package com.example.edtai.infrastructure.settings;

import java.nio.file.Path;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;

public final class FeedbackLocation {

    private FeedbackLocation() {
    }

    public static Path resolve() {
        // Определяет каталог отзывов и правил
        String configured = Platform.getPreferencesService()
                .getString(PreferenceKeys.NODE, PreferenceKeys.FEEDBACK_DIRECTORY, "", null);
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured.strip());
        }
        IPath workspace = ResourcesPlugin.getWorkspace().getRoot().getLocation();
        Path base = workspace == null ? Path.of(System.getProperty("user.home")) : workspace.toFile().toPath();
        return base.resolve(".perun");
    }
}
