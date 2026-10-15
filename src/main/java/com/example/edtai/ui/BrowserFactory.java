package com.example.edtai.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;

final class BrowserFactory {

    private BrowserFactory() {
    }

    static Browser create(Composite parent) {
        // Создает браузер с современным движком
        if ("win32".equals(SWT.getPlatform())) {
            try {
                return new Browser(parent, SWT.EDGE);
            } catch (SWTError e) {
                return new Browser(parent, SWT.NONE);
            }
        }
        return new Browser(parent, SWT.NONE);
    }
}
