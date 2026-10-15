package com.example.edtai.infrastructure.settings;

import java.util.function.Consumer;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.InstanceScope;

public final class SettingsStore {

    private final IEclipsePreferences node = InstanceScope.INSTANCE.getNode(PreferenceKeys.NODE);
    private IPreferenceChangeListener listener;

    public PluginSettings load() {
        // Читает текущие настройки плагина
        return new PluginSettings(
                read(PreferenceKeys.COMMAND),
                read(PreferenceKeys.ARGUMENTS),
                read(PreferenceKeys.WORK_DIRECTORY),
                Boolean.parseBoolean(read(PreferenceKeys.DEMO_MODE)),
                read(PreferenceKeys.THEME));
    }

    public void onChange(Consumer<String> action) {
        // Подписывается на изменение настроек
        dispose();
        listener = event -> action.accept(event.getKey());
        node.addPreferenceChangeListener(listener);
    }

    public void dispose() {
        // Снимает подписку на настройки
        if (listener != null) {
            node.removePreferenceChangeListener(listener);
            listener = null;
        }
    }

    private String read(String key) {
        // Читает значение с учетом умолчаний
        String value = Platform.getPreferencesService().getString(PreferenceKeys.NODE, key, "", null);
        return value == null ? "" : value.strip();
    }
}
