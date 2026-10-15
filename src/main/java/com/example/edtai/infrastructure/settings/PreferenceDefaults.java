package com.example.edtai.infrastructure.settings;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

public final class PreferenceDefaults extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        // Задает значения настроек по умолчанию
        IEclipsePreferences defaults = DefaultScope.INSTANCE.getNode(PreferenceKeys.NODE);
        defaults.put(PreferenceKeys.COMMAND, "");
        defaults.put(PreferenceKeys.ARGUMENTS, "");
        defaults.put(PreferenceKeys.WORK_DIRECTORY, "");
        defaults.putBoolean(PreferenceKeys.DEMO_MODE, false);
        defaults.put(PreferenceKeys.THEME, "auto");
        defaults.put(PreferenceKeys.ALLOWED_TOOLS, "");
        defaults.put(PreferenceKeys.DENIED_TOOLS, "");
    }
}
