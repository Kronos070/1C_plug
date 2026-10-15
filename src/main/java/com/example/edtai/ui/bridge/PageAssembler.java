package com.example.edtai.ui.bridge;

import com.example.edtai.ui.theme.EdtPalette;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class PageAssembler {

    private static final List<String> STYLES = List.of("tokens.css", "base.css", "header.css", "messages.css",
            "empty.css", "markdown.css", "composer.css", "menus.css", "cards.css", "toasts.css",
            "palette.css", "context.css");
    private static final List<String> SCRIPTS = List.of("core.js", "icons.js", "highlight.js", "markdown.js",
            "menus.js", "notices.js", "history.js", "header.js", "parts.js", "reply.js", "messages.js", "stream.js",
            "requests.js", "commands.js", "palette.js", "mentions.js", "login.js", "chips.js", "context.js",
            "composer.js", "app.js");

    private PageAssembler() {
    }

    public static String assemble(EdtPalette palette) {
        // Собирает HTML со встроенными ресурсами
        StringBuilder styles = new StringBuilder();
        STYLES.forEach(name -> styles.append(read("css/" + name)).append('\n'));
        StringBuilder scripts = new StringBuilder();
        SCRIPTS.forEach(name -> scripts.append(read("js/" + name)).append('\n'));
        String background = palette.background().isBlank() ? "" : "--edt-bg:" + palette.background() + ";";
        return read("index.html")
                .replace("{{theme}}", palette.dark() ? "dark" : "light")
                .replace("{{background}}", background)
                .replace("/*{{styles}}*/", styles.toString())
                .replace("/*{{scripts}}*/", scripts.toString());
    }

    private static String read(String name) {
        // Читает ресурс страницы из плагина
        try (InputStream in = PageAssembler.class.getResourceAsStream("/web/" + name)) {
            if (in == null) {
                throw new IllegalStateException("Не найден ресурс страницы: " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
