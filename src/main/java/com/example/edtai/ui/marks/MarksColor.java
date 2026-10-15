package com.example.edtai.ui.marks;

import com.example.edtai.infrastructure.settings.PreferenceKeys;
import java.util.List;
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

    private static final String AUTO = "marks.autoColor.";
    private static final String LEGACY_AUTO = "marks.autoColor";
    private static final RGB ACCENT = new RGB(120, 132, 230);
    private static final RGB ORANGE = new RGB(232, 146, 52);
    private static final List<Tint> TINTS = List.of(new Tint("perunAiColor", ACCENT, 0.08, 0.14),
            new Tint("perunAiAddedColor", ACCENT, 0.10, 0.17), new Tint("perunAiModifiedColor", ORANGE, 0.13, 0.20));

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
        // Ставит едва заметные оттенки фона редактора
        IPreferenceStore store = EditorsUI.getPreferenceStore();
        IEclipsePreferences node = InstanceScope.INSTANCE.getNode(PreferenceKeys.NODE);
        RGB background = background(store, display);
        boolean dark = (background.red * 299 + background.green * 587 + background.blue * 114) / 1000 < 128;
        for (Tint tint : TINTS) {
            String current = store.getString(tint.key());
            String auto = node.get(AUTO + tint.key(), node.get(LEGACY_AUTO, ""));
            if (!store.isDefault(tint.key()) && !current.equals(auto)) {
                continue;
            }
            String value = StringConverter.asString(mix(background, tint.color(), dark ? tint.dark() : tint.light()));
            node.put(AUTO + tint.key(), value);
            store.setValue(tint.key(), value);
        }
    }

    private static RGB background(IPreferenceStore store, Display display) {
        // Определяет фон текстового редактора
        if (store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT)
                || store.getString(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND).isEmpty()) {
            return display.getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB();
        }
        return PreferenceConverter.getColor(store, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);
    }

    private static RGB mix(RGB base, RGB color, double share) {
        // Смешивает фон с цветом метки
        return new RGB(channel(base.red, color.red, share), channel(base.green, color.green, share),
                channel(base.blue, color.blue, share));
    }

    private static int channel(int base, int accent, double share) {
        // Смешивает один канал цвета
        return (int) Math.round(base * (1 - share) + accent * share);
    }

    private record Tint(String key, RGB color, double light, double dark) {
    }
}
