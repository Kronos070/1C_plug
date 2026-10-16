package com.example.edtai.ui.docs;

import com.example.edtai.application.DocsSignals;
import com.example.edtai.bootstrap.DocsAssembler;
import com.example.edtai.infrastructure.settings.SettingsStore;
import com.example.edtai.ui.BrowserFactory;
import com.example.edtai.ui.bridge.PageAssembler;
import com.example.edtai.ui.bridge.WebBridge;
import com.example.edtai.ui.theme.EdtPalette;
import com.example.edtai.ui.theme.ThemeWatcher;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.part.EditorPart;

public final class DocsEditor extends EditorPart {

    public static final String ID = "com.example.edtai.editor.docs";

    private final SettingsStore settings = new SettingsStore();
    private Browser browser;
    private DocsCommandRouter router;
    private ThemeWatcher theme;
    private Runnable unsubscribe = () -> {
    };
    private String pending = "";

    @Override
    public void init(IEditorSite site, IEditorInput input) {
        // Привязывает вкладку к рабочему месту
        setSite(site);
        setInput(input);
        setPartName(input.getName());
        setTitleToolTip(input.getToolTipText());
    }

    @Override
    public void createPartControl(Composite parent) {
        // Создает браузер страницы документации
        parent.setLayout(new FillLayout());
        browser = BrowserFactory.create(parent);
        WebBridge bridge = new WebBridge(browser);
        router = new DocsCommandRouter(DocsAssembler.service(), bridge, parent.getShell());
        bridge.onCommand(router);
        browser.setText(PageAssembler.docs(EdtPalette.from(parent).prefer(settings.load().theme())));
        theme = new ThemeWatcher(parent, getSite().getService(IEventBroker.class),
                palette -> bridge.theme(palette.prefer(settings.load().theme())));
        unsubscribe = DocsSignals.subscribe(router::status);
        if (!pending.isBlank()) {
            router.show(pending);
        }
    }

    public void show(String id) {
        // Открывает запись документации
        if (router == null) {
            pending = id;
        } else {
            router.show(id);
        }
    }

    @Override
    public void setFocus() {
        // Передает фокус странице
        if (browser != null && !browser.isDisposed()) {
            browser.setFocus();
        }
    }

    @Override
    public void dispose() {
        // Отписывается от изменений и темы
        unsubscribe.run();
        settings.dispose();
        if (theme != null) {
            theme.dispose();
        }
        super.dispose();
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        // Правки сохраняются сразу в Перун
    }

    @Override
    public void doSaveAs() {
        // Сохранение как не поддерживается
    }

    @Override
    public boolean isDirty() {
        // Вкладка не держит несохраненных данных
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        // Сохранение как не поддерживается
        return false;
    }
}
