package com.example.edtai.ui.theme;

import java.util.Locale;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;

public record EdtPalette(boolean dark, String background, String foreground) {

    private static final String THEME_NODE = "org.eclipse.e4.ui.css.swt.theme";

    public static EdtPalette from(Control control) {
        // Вычисляет палитру по цветам EDT
        RGB background = rgb(control.getBackground());
        RGB foreground = rgb(control.getForeground());
        boolean darkTheme = themeId().contains("dark");
        boolean darkColor = luminance(background) < 0.45;
        if (darkTheme && !darkColor) {
            return new EdtPalette(true, "", "");
        }
        return new EdtPalette(darkColor, hex(background), hex(foreground));
    }

    public EdtPalette prefer(String mode) {
        // Применяет выбранную пользователем тему
        if ("dark".equals(mode) && !dark) {
            return new EdtPalette(true, "", "");
        }
        if ("light".equals(mode) && dark) {
            return new EdtPalette(false, "", "");
        }
        return this;
    }

    private static String themeId() {
        // Читает идентификатор темы Eclipse
        String id = Platform.getPreferencesService().getString(THEME_NODE, "themeid", "", null);
        return id == null ? "" : id.toLowerCase(Locale.ROOT);
    }

    private static RGB rgb(Color color) {
        // Безопасно получает RGB цвета
        return color == null ? new RGB(255, 255, 255) : color.getRGB();
    }

    private static double luminance(RGB rgb) {
        // Считает относительную яркость цвета
        return (0.2126 * rgb.red + 0.7152 * rgb.green + 0.0722 * rgb.blue) / 255.0;
    }

    private static String hex(RGB rgb) {
        // Переводит цвет в HEX-строку
        return String.format("#%02x%02x%02x", rgb.red, rgb.green, rgb.blue);
    }
}
