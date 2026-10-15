package com.example.edtai.ui.marks;

import com.example.edtai.infrastructure.settings.PreferenceKeys;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.editors.text.EditorsUI;
import org.osgi.service.prefs.BackingStoreException;

public final class MarksVisibility {

    private static final String[] KEYS = {"perunAiHighlight", "perunAiRuler", "perunAiOverview"};

    private MarksVisibility() {
    }

    public static boolean isVisible() {
        // Читает, показываются ли метки ИИ
        return Platform.getPreferencesService().getBoolean(PreferenceKeys.NODE, PreferenceKeys.MARKS_VISIBLE, true,
                null);
    }

    public static void set(boolean visible) {
        // Сохраняет и применяет видимость меток
        IEclipsePreferences node = InstanceScope.INSTANCE.getNode(PreferenceKeys.NODE);
        node.putBoolean(PreferenceKeys.MARKS_VISIBLE, visible);
        try {
            node.flush();
        } catch (BackingStoreException ignored) {
            apply(visible);
            return;
        }
        apply(visible);
    }

    public static void apply(boolean visible) {
        // Включает или скрывает подсветку в редакторах
        IPreferenceStore store = EditorsUI.getPreferenceStore();
        for (String key : KEYS) {
            store.setValue(key, visible);
        }
    }
}
