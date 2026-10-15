package com.example.edtai.ui.bridge;

import com.example.edtai.ui.theme.EdtPalette;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class PageAssembler {

    private static final List<String> CHAT_STYLES = List.of(
            "tokens.css", "base.css", "header.css", "messages.css", "empty.css", "markdown.css", "composer.css",
            "menus.css", "cards.css", "toasts.css", "palette.css", "context.css", "logo.css", "feedback.css");
    private static final List<String> CHAT_SCRIPTS = List.of(
            "core.js", "icons.js", "theme.js", "logo-model.js", "logo-render.js", "logo-timeline.js", "logo.js",
            "logo-egg.js", "highlight.js", "markdown.js", "menus.js", "notices.js", "history.js", "header.js",
            "sources.js", "parts.js", "rating.js", "reply.js", "messages.js", "stream.js", "requests.js",
            "commands.js", "palette.js", "mentions.js", "login.js", "chips.js", "context.js", "composer.js",
            "app.js");
    private static final List<String> DOCS_STYLES = List.of(
            "tokens.css", "base.css", "markdown.css", "toasts.css", "logo.css", "docs-layout.css", "docs-side.css",
            "docs-tree.css", "docs-page.css", "docs-cases.css", "docs-search.css", "docs-home.css", "docs-edit.css",
            "docs-graph.css");
    private static final List<String> DOCS_SCRIPTS = List.of(
            "core.js", "icons.js", "theme.js", "logo-model.js", "logo-render.js", "logo-timeline.js", "logo.js",
            "highlight.js", "markdown.js", "docs-kinds.js", "docs-tree.js", "docs-outdated.js", "docs-search.js",
            "docs-parts.js", "docs-graph.js", "docs-page.js", "docs-dispute.js", "docs-edit.js", "docs-overview.js",
            "docs-resize.js", "docs-app.js", "docs-events.js");

    private PageAssembler() {
    }

    public static String assemble(EdtPalette palette) {
        // Собирает HTML страницы чата
        return build("index.html", CHAT_STYLES, CHAT_SCRIPTS, palette);
    }

    public static String docs(EdtPalette palette) {
        // Собирает HTML страницы документации
        return build("docs.html", DOCS_STYLES, DOCS_SCRIPTS, palette);
    }

    private static String build(String page, List<String> css, List<String> js, EdtPalette palette) {
        // Встраивает стили и скрипты в шаблон
        StringBuilder styles = new StringBuilder();
        css.forEach(name -> styles.append(read("css/" + name)).append('\n'));
        StringBuilder scripts = new StringBuilder();
        js.forEach(name -> scripts.append(read("js/" + name)).append('\n'));
        String background = palette.background().isBlank() ? "" : "--edt-bg:" + palette.background() + ";";
        return read(page)
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
