package com.example.edtai.infrastructure.settings;

import com.example.edtai.application.port.PermissionStore;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

public final class PreferencePermissionStore implements PermissionStore {

    @Override
    public Set<String> allowed() {
        // Возвращает всегда разрешенные инструменты
        return read(PreferenceKeys.ALLOWED_TOOLS);
    }

    @Override
    public Set<String> denied() {
        // Возвращает всегда запрещенные инструменты
        return read(PreferenceKeys.DENIED_TOOLS);
    }

    @Override
    public synchronized void remember(String key, boolean allow) {
        // Сохраняет постоянное решение по инструменту
        Set<String> allowed = allowed();
        Set<String> denied = denied();
        (allow ? allowed : denied).add(key);
        (allow ? denied : allowed).remove(key);
        IEclipsePreferences node = InstanceScope.INSTANCE.getNode(PreferenceKeys.NODE);
        node.put(PreferenceKeys.ALLOWED_TOOLS, String.join(", ", allowed));
        node.put(PreferenceKeys.DENIED_TOOLS, String.join(", ", denied));
        try {
            node.flush();
        } catch (BackingStoreException ignored) {
            return;
        }
    }

    private static Set<String> read(String key) {
        // Читает список инструментов из настроек
        String value = Platform.getPreferencesService().getString(PreferenceKeys.NODE, key, "", null);
        Set<String> result = new LinkedHashSet<>();
        Arrays.stream(value.split(",")).map(String::strip).filter(item -> !item.isEmpty()).forEach(result::add);
        return result;
    }
}
