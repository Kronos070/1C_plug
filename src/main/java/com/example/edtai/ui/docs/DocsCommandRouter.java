package com.example.edtai.ui.docs;

import com.example.edtai.application.DocsService;
import com.example.edtai.domain.AiProvenance;
import com.example.edtai.domain.DocSection;
import com.example.edtai.ui.bridge.ClipboardText;
import com.example.edtai.ui.bridge.WebBridge;
import com.example.edtai.ui.feedback.EditorInserter;
import com.google.gson.JsonObject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Shell;

final class DocsCommandRouter implements Consumer<JsonObject> {

    private final DocsService service;
    private final WebBridge bridge;
    private final Shell shell;

    DocsCommandRouter(DocsService service, WebBridge bridge, Shell shell) {
        this.service = service;
        this.bridge = bridge;
        this.shell = shell;
    }

    @Override
    public void accept(JsonObject command) {
        // Выполняет команду страницы документации
        String id = DocsMessages.text(command, "id");
        switch (DocsMessages.text(command, "action")) {
            case "ready" -> ready();
            case "docsStatus" -> deliver("status", "", service.status());
            case "docsSync" -> deliver("status", "", service.sync());
            case "docsTree" -> tree(command);
            case "docsPage" -> deliver("page", id, service.page(id));
            case "docsSearch" -> deliver("search", DocsMessages.text(command, "query"), service.search(
                    DocsMessages.text(command, "query"), DocSection.fromId(DocsMessages.text(command, "kind"))));
            case "docsOutdated" -> deliver("outdated", "", service.outdated());
            case "docsCallers" -> deliver("callers", id, service.callers(id, DocsMessages.flag(command, "exported"))
                    .thenApply(DocsMessages::callers));
            case "docsSave" -> deliver("saved", id, service.save(id, DocsMessages.draft(command)));
            case "docsRemove" -> deliver("saved", id, service.remove(id));
            case "docsOpenSource" -> CodeOpener.open(DocsMessages.text(command, "path"),
                    DocsMessages.number(command, "line")).ifPresent(message -> notice("warning", message));
            case "askChat" -> ChatPrompt.send(DocsMessages.text(command, "text"))
                    .ifPresent(message -> notice("warning", message));
            case "copy" -> ClipboardText.copy(shell, DocsMessages.text(command, "text"));
            case "insert" -> EditorInserter.insert(DocsMessages.text(command, "code"), new AiProvenance("", "", "docs"))
                    .ifPresent(message -> notice("warning", message));
            case "link" -> openLink(DocsMessages.text(command, "url"));
            default -> {
            }
        }
    }

    void status(String text) {
        // Пересылает статус индексации на страницу
        if (text.isEmpty()) {
            bridge.post(DocsMessages.availability(service.available()));
        }
        bridge.post(DocsMessages.signal(text));
    }

    void show(String id) {
        // Просит страницу открыть запись
        bridge.post(DocsMessages.open(id));
    }

    private void ready() {
        // Сообщает о поддержке и состоянии индекса
        bridge.post(DocsMessages.availability(service.available()));
        if (service.available()) {
            deliver("status", "", service.status());
        }
    }

    private void tree(JsonObject command) {
        // Запрашивает узлы дерева раздела
        String section = DocsMessages.text(command, "section");
        String node = DocsMessages.text(command, "node");
        Optional<DocSection> value = DocSection.fromId(section);
        if (value.isEmpty()) {
            notice("error", "Неизвестный раздел документации: " + section);
            return;
        }
        deliver("tree", section + "|" + node, service.tree(value.get(), node));
    }

    private void deliver(String kind, String key, CompletableFuture<JsonObject> future) {
        // Отправляет ответ или ошибку на страницу
        future.thenAccept(data -> bridge.post(DocsMessages.loaded(kind, key, data))).exceptionally(error -> {
            Throwable cause = error instanceof CompletionException && error.getCause() != null
                    ? error.getCause() : error;
            JsonObject failed = DocsMessages.failed(kind, key,
                    cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage());
            bridge.post(failed);
            return null;
        });
    }

    private void notice(String level, String text) {
        // Отправляет уведомление на страницу
        bridge.post(DocsMessages.notice(level, text));
    }

    private static void openLink(String url) {
        // Открывает ссылку во внешнем браузере
        if (url.startsWith("http://") || url.startsWith("https://")) {
            Program.launch(url);
        }
    }
}
