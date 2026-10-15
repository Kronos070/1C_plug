package com.example.edtai.ui.theme;

import java.util.function.Consumer;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.widgets.Composite;
import org.osgi.service.event.EventHandler;

public final class ThemeWatcher {

    private static final String THEME_CHANGED = "org/eclipse/e4/ui/css/swt/theme/ThemeManager/themeChanged";
    private static final int SETTLE_DELAY_MS = 700;

    private final Composite parent;
    private final IEventBroker broker;
    private final Consumer<EdtPalette> consumer;
    private final EventHandler handler;

    public ThemeWatcher(Composite parent, IEventBroker broker, Consumer<EdtPalette> consumer) {
        this.parent = parent;
        this.broker = broker;
        this.consumer = consumer;
        this.handler = event -> schedule();
        if (broker != null) {
            broker.subscribe(THEME_CHANGED, handler);
        }
        schedule();
    }

    public void dispose() {
        // Отписывается от смены темы
        if (broker != null) {
            broker.unsubscribe(handler);
        }
    }

    public void refresh() {
        // Перечитывает палитру по запросу
        schedule();
    }

    private void schedule() {
        // Публикует палитру после применения стилей
        if (parent.isDisposed()) {
            return;
        }
        parent.getDisplay().asyncExec(this::publish);
        parent.getDisplay().timerExec(SETTLE_DELAY_MS, this::publish);
    }

    private void publish() {
        // Передает текущую палитру подписчику
        if (!parent.isDisposed()) {
            consumer.accept(EdtPalette.from(parent));
        }
    }
}
