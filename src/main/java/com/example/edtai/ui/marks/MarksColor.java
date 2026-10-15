package com.example.edtai.ui.marks;

import com.example.edtai.infrastructure.settings.PreferenceKeys;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;

public final class MarksColor {

    private static final String COLOR = "perunAiColor";
    private static final String AUTO = "marks.autoColor";
    private static final RGB ACCENT = new RGB(120, 132, 230);
    private static final double LIGHT_MIX = 0.08;
    private static final double DARK_MIX = 0.14;

    private MarksColor() {
    }

    public static void install(Display display) {
        // Подбирает мягкий цвет и следит за темой
        apply(display);
        EditorsUI.getPreferenceStore().addPropertyChangeListener(event -> {
            if (event.getProperty().startsWith(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND)) {
                display.asyncExec(() -> apply(display));
            }
        });
    }

    private static void apply(Display display) {
        // Ставит едва заметный оттенок фона редактора
        IPreferenceStore store = EditorsUI.getPreferenceStore();
        IEclipsePreferences node = InstanceScope.INSTANCE.getNode(PreferenceKeys.NODE);
        String current = store.getString(COLOR);
        String auto = node.get(AUTO, "");
        if (!store.isDefault(COLOR) && !current.equals(auto)) {
            return;
        }
        RGB background = background(store, display);
        boolean dark = (background.red * 299 + background.green * 587 + background.blue * 114) / 1000 < 128;
        String tint = StringConverter.asString(mix(background, dark ? DARK_MIX : LIGHT_MIX));
        node.put(AUTO, tint);
        store.setValue(COLOR, tint);
    }

    private static RGB background(IPreferenceStore store, Display display) {
        // Определяет фон текстового редактора
        if (store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT)
                || store.getString(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND).isEmpty()) {
            return display.getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB();
        }
        return PreferenceConverter.getColor(store, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);
    }

    private static RGB mix(RGB base, double share) {
        // Смешивает фон с акцентным цветом
        return new RGB(channel(base.red, ACCENT.red, share), channel(base.green, ACCENT.green, share),
                channel(base.blue, ACCENT.blue, share));
    }

    private static int channel(int base, int accent, double share) {
        // Смешивает один канал цвета
        return (int) Math.round(base * (1 - share) + accent * share);
    }
}
