package com.example.edtai.ui;

import com.example.edtai.application.ChatService;
import com.example.edtai.application.ReviewSignals;
import com.example.edtai.bootstrap.ChatAssembler;
import com.example.edtai.bootstrap.FeedbackAssembler;
import com.example.edtai.domain.CodeAction;
import com.example.edtai.domain.CodeContext;
import com.example.edtai.infrastructure.settings.PreferenceKeys;
import com.example.edtai.infrastructure.settings.SettingsStore;
import com.example.edtai.ui.bridge.PageAssembler;
import com.example.edtai.ui.bridge.WebBridge;
import com.example.edtai.ui.bridge.WebCommandRouter;
import com.example.edtai.ui.context.EditorContextTracker;
import com.example.edtai.ui.layout.ChatSlot;
import com.example.edtai.ui.review.ReviewViews;
import com.example.edtai.ui.theme.EdtPalette;
import com.example.edtai.ui.theme.ThemeWatcher;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public final class ChatView extends ViewPart {

    private static final int RESTART_DELAY_MS = 400;

    private final SettingsStore settings = new SettingsStore();
    private final Runnable restartTask = this::restart;
    private Composite parent;
    private Browser browser;
    private WebBridge bridge;
    private ThemeWatcher theme;
    private EditorContextTracker contexts;
    private Runnable pendingAsk;
    private Runnable unsubscribeReview = () -> {
    };
    private volatile ChatService service;

    @Override
    public void createPartControl(Composite container) {
        // Готовит вид и растягивает на высоту
        parent = container;
        parent.setLayout(new FillLayout());
        Optional<Runnable> move = ChatSlot.pendingMove(getSite());
        if (move.isEmpty()) {
            buildContent();
            return;
        }
        parent.getDisplay().asyncExec(() -> {
            try {
                move.get().run();
            } finally {
                if (!parent.isDisposed()) {
                    buildContent();
                }
            }
        });
    }

    private void buildContent() {
        // Создает браузер чата и сервисы
        browser = BrowserFactory.create(parent);
        bridge = new WebBridge(browser);
        contexts = new EditorContextTracker(getSite().getPage(), bridge::context);
        bridge.onCommand(new WebCommandRouter(() -> service, contexts::resolve, this::scheduleRestart,
                parent.getShell(), FeedbackAssembler.service(), bridge::render, bridge::post));
        browser.setText(PageAssembler.assemble(EdtPalette.from(parent).prefer(settings.load().theme())));
        theme = new ThemeWatcher(parent, getSite().getService(IEventBroker.class),
                palette -> bridge.theme(palette.prefer(settings.load().theme())));
        settings.onChange(this::onSettingChanged);
        unsubscribeReview = ReviewSignals.subscribe(files -> bridge.post(ReviewViews.summary(files)));
        parent.layout(true, true);
        connect();
        if (pendingAsk != null) {
            pendingAsk.run();
            pendingAsk = null;
        }
    }

    public void ask(CodeAction action, CodeContext context) {
        // Отправляет действие с кодом в чат
        if (bridge == null) {
            pendingAsk = () -> ask(action, context);
            return;
        }
        contexts.pin(context);
        bridge.ask(action, context);
    }

    public void prompt(String text) {
        // Отправляет текстовый запрос в чат
        if (bridge == null) {
            pendingAsk = () -> prompt(text);
            return;
        }
        bridge.prompt(text);
    }

    @Override
    public void setFocus() {
        // Передает фокус полю ввода
        if (browser != null && !browser.isDisposed()) {
            browser.setFocus();
            browser.execute("window.Perun && window.Perun.focusInput && window.Perun.focusInput();");
        }
    }

    @Override
    public void dispose() {
        // Освобождает ресурсы и агента
        settings.dispose();
        unsubscribeReview.run();
        if (contexts != null) {
            contexts.dispose();
        }
        if (theme != null) {
            theme.dispose();
        }
        ChatService current = service;
        service = null;
        if (current != null) {
            CompletableFuture.runAsync(current::stop);
        }
        super.dispose();
    }

    private void connect() {
        // Создает сервис и запускает агента
        ChatService next = ChatAssembler.assemble(settings.load(), bridge);
        service = next;
        CompletableFuture.runAsync(next::start);
    }

    private void onSettingChanged(String key) {
        // Реагирует на изменение настройки
        if (key.startsWith("permissions.") || key.startsWith("feedback.") || key.startsWith("marks.")) {
            return;
        }
        if (PreferenceKeys.THEME.equals(key)) {
            parent.getDisplay().asyncExec(() -> {
                if (!parent.isDisposed()) {
                    theme.refresh();
                }
            });
        } else {
            scheduleRestart();
        }
    }

    private void scheduleRestart() {
        // Откладывает перезапуск после изменений
        if (parent != null && !parent.isDisposed()) {
            parent.getDisplay().asyncExec(() -> {
                if (!parent.isDisposed()) {
                    parent.getDisplay().timerExec(RESTART_DELAY_MS, restartTask);
                }
            });
        }
    }

    private void restart() {
        // Перезапускает агента с новыми настройками
        if (parent == null || parent.isDisposed()) {
            return;
        }
        ChatService previous = service;
        CompletableFuture.runAsync(() -> {
            if (previous != null) {
                previous.stop();
            }
        }).thenRun(() -> parent.getDisplay().asyncExec(() -> {
            if (!parent.isDisposed()) {
                connect();
            }
        }));
    }
}
