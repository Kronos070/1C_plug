package com.example.edtai.ui.bridge;

import com.example.edtai.application.port.ChatOutput;
import com.example.edtai.domain.ChatEvent;
import com.example.edtai.domain.CodeAction;
import com.example.edtai.domain.CodeContext;
import com.example.edtai.ui.theme.EdtPalette;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Display;

public final class WebBridge implements ChatOutput {

    private static final String FUNCTION = "perunPost";

    private final Browser browser;
    private final Display display;
    private final List<JsonObject> queue = new ArrayList<>();
    private Consumer<JsonObject> handler = command -> {
    };
    private boolean ready;
    private boolean scheduled;

    public WebBridge(Browser browser) {
        this.browser = browser;
        this.display = browser.getDisplay();
        new BrowserFunction(browser, FUNCTION) {
            @Override
            public Object function(Object[] arguments) {
                // Принимает команду со страницы
                receive(arguments);
                return null;
            }
        };
    }

    public void onCommand(Consumer<JsonObject> target) {
        // Задает обработчик команд страницы
        handler = target;
    }

    @Override
    public void render(ChatEvent event) {
        // Отправляет событие чата на страницу
        post(EventSerializer.toJson(event));
    }

    public void theme(EdtPalette palette) {
        // Отправляет палитру EDT на страницу
        post(EventSerializer.theme(palette));
    }

    public void context(Optional<CodeContext> context) {
        // Отправляет прикрепленный код на страницу
        post(EventSerializer.context(context));
    }

    public void ask(CodeAction action, CodeContext context) {
        // Отправляет действие с кодом на страницу
        post(EventSerializer.ask(action, context));
    }

    public void post(JsonObject message) {
        // Ставит сообщение в очередь отправки
        synchronized (queue) {
            queue.add(message);
            if (!ready || scheduled) {
                return;
            }
            scheduled = true;
        }
        schedule();
    }

    private void receive(Object[] arguments) {
        // Разбирает и маршрутизирует команду
        if (arguments.length == 0 || !(arguments[0] instanceof String raw)) {
            return;
        }
        try {
            JsonObject command = JsonParser.parseString(raw).getAsJsonObject();
            if ("ready".equals(command.has("action") ? command.get("action").getAsString() : "")) {
                markReady();
            }
            handler.accept(command);
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException e) {
            return;
        }
    }

    private void markReady() {
        // Открывает очередь после загрузки
        synchronized (queue) {
            ready = true;
            scheduled = true;
        }
        schedule();
    }

    private void schedule() {
        // Планирует отправку в UI-потоке
        if (!display.isDisposed()) {
            display.asyncExec(this::flush);
        }
    }

    private void flush() {
        // Передает пачку сообщений в браузер
        JsonArray batch = new JsonArray();
        synchronized (queue) {
            queue.forEach(batch::add);
            queue.clear();
            scheduled = false;
        }
        if (browser.isDisposed() || batch.size() == 0) {
            return;
        }
        String payload = batch.toString().replace("\u2028", "\\u2028").replace("\u2029", "\\u2029");
        browser.execute("window.Perun && window.Perun.receiveAll(" + payload + ");");
    }
}
