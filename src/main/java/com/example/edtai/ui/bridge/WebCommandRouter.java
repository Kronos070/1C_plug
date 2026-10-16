package com.example.edtai.ui.bridge;

import com.example.edtai.application.ChatService;
import com.example.edtai.application.FeedbackService;
import com.example.edtai.bootstrap.DocsAssembler;
import com.example.edtai.bootstrap.ReviewAssembler;
import com.example.edtai.domain.AgentMode;
import com.example.edtai.domain.AiProvenance;
import com.example.edtai.domain.ChatEvent;
import com.example.edtai.domain.CodeContext;
import com.example.edtai.domain.UiAnswer;
import com.example.edtai.ui.docs.DocsOpener;
import com.example.edtai.ui.review.ReviewActions;
import com.example.edtai.ui.review.ReviewViews;
import com.example.edtai.ui.feedback.EditorInserter;
import com.google.gson.JsonObject;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;

public final class WebCommandRouter implements Consumer<JsonObject> {

    private static final String PREFERENCE_PAGE = "com.example.edtai.preferences";

    private final Supplier<ChatService> service;
    private final Function<String, Optional<CodeContext>> contexts;
    private final Runnable restart;
    private final Shell shell;
    private final ChatFeedback feedback;
    private final Consumer<JsonObject> post;
    private final Consumer<ChatEvent> notify;

    public WebCommandRouter(Supplier<ChatService> service, Function<String, Optional<CodeContext>> contexts,
            Runnable restart, Shell shell, FeedbackService feedback, Consumer<ChatEvent> notify,
            Consumer<JsonObject> post) {
        this.post = post;
        this.notify = notify;
        this.service = service;
        this.contexts = contexts;
        this.restart = restart;
        this.shell = shell;
        this.feedback = new ChatFeedback(feedback, notify);
    }

    @Override
    public void accept(JsonObject command) {
        // Выполняет команду со страницы
        String action = text(command, "action");
        switch (action) {
            case "copy" -> ClipboardText.copy(shell, text(command, "text"));
            case "ready" -> post.accept(ReviewViews.summary(ReviewAssembler.service().files()));
            case "reviewAccept" -> ReviewActions.accept(ReviewAssembler.service(), text(command, "path"));
            case "reviewReject" -> ReviewActions.reject(ReviewAssembler.service(), text(command, "path"), shell,
                    this::notice);
            case "reviewOpen" -> ReviewActions.open(text(command, "path"), number(command, "line"), this::notice);
            case "docs" -> DocsOpener.open(text(command, "id"));
            case "docsPeek" -> DocsPeek.peek(DocsAssembler.service(), command.get("ids"), text(command, "key"), post);
            case "link" -> openLink(text(command, "url"));
            case "settings" -> openSettings();
            case "retry" -> restart.run();
            case "insert" -> EditorInserter.insert(text(command, "code"), provenance(command, "insert"))
                    .ifPresent(feedback::warn);
            case "rateChat" -> feedback.rate(text(command, "rating"), text(command, "comment"),
                    text(command, "answer"), provenance(command, "chat"));
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
            case "send" -> chat.send(text(command, "text"), contexts.apply(text(command, "contextId")),
                    AgentMode.fromId(text(command, "mode")));
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

    private void notice(String level, String text) {
        // Показывает уведомление в чате
        notify.accept(new ChatEvent.Notice(level, text));
    }

    private static int number(JsonObject command, String key) {
        // Читает числовое поле команды
        return command.has(key) && command.get(key).isJsonPrimitive() ? command.get(key).getAsInt() : 0;
    }

    private static AiProvenance provenance(JsonObject command, String source) {
        // Собирает происхождение кода из команды
        return new AiProvenance(text(command, "model"), text(command, "prompt"), source);
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
