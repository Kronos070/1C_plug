package com.example.edtai.ui.bridge;

import com.example.edtai.application.ChatService;
import com.example.edtai.domain.UiAnswer;
import com.google.gson.JsonObject;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;

public final class WebCommandRouter implements Consumer<JsonObject> {

    private static final String PREFERENCE_PAGE = "com.example.edtai.preferences";

    private final Supplier<ChatService> service;
    private final Runnable restart;
    private final Shell shell;

    public WebCommandRouter(Supplier<ChatService> service, Runnable restart, Shell shell) {
        this.service = service;
        this.restart = restart;
        this.shell = shell;
    }

    @Override
    public void accept(JsonObject command) {
        // Выполняет команду со страницы
        String action = text(command, "action");
        switch (action) {
            case "copy" -> copy(text(command, "text"));
            case "link" -> openLink(text(command, "url"));
            case "settings" -> openSettings();
            case "retry" -> restart.run();
            default -> dispatch(action, command);
        }
    }

    private void dispatch(String action, JsonObject command) {
        // Передает команду сервису чата
        ChatService chat = service.get();
        if (chat == null) {
            return;
        }
        switch (action) {
            case "send" -> chat.send(text(command, "text"));
            case "abort" -> chat.abort();
            case "newChat" -> chat.newChat();
            case "sessions" -> chat.listSessions();
            case "files" -> chat.findFiles(text(command, "query"));
            case "openSession" -> chat.openSession(text(command, "path"));
            case "model" -> chat.settings().selectModel(text(command, "key"));
            case "thinking" -> chat.settings().selectThinking(text(command, "id"));
            case "mode" -> chat.settings().selectMode(text(command, "id"));
            case "login" -> chat.login(text(command, "provider"), text(command, "key")).thenRun(restart);
            case "answer" -> chat.answer(answer(command), text(command, "scope"));
            default -> {
            }
        }
    }

    private UiAnswer answer(JsonObject command) {
        // Собирает ответ на запрос агента
        String id = text(command, "id");
        if (command.has("cancelled") && command.get("cancelled").getAsBoolean()) {
            return UiAnswer.cancel(id);
        }
        if (command.has("confirmed")) {
            return UiAnswer.confirm(id, command.get("confirmed").getAsBoolean());
        }
        return UiAnswer.value(id, text(command, "value"));
    }

    private void copy(String value) {
        // Копирует текст в буфер обмена
        if (value.isEmpty() || shell.isDisposed()) {
            return;
        }
        Clipboard clipboard = new Clipboard(shell.getDisplay());
        try {
            clipboard.setContents(new Object[] {value}, new Transfer[] {TextTransfer.getInstance()});
        } finally {
            clipboard.dispose();
        }
    }

    private void openLink(String url) {
        // Открывает ссылку во внешнем браузере
        if (url.startsWith("http://") || url.startsWith("https://")) {
            Program.launch(url);
        }
    }

    private void openSettings() {
        // Открывает страницу настроек плагина
        if (!shell.isDisposed()) {
            String[] pages = {PREFERENCE_PAGE};
            PreferencesUtil.createPreferenceDialogOn(shell, PREFERENCE_PAGE, pages, null).open();
        }
    }

    private static String text(JsonObject command, String key) {
        // Читает строковое поле команды
        return command.has(key) && command.get(key).isJsonPrimitive() ? command.get(key).getAsString() : "";
    }
}
