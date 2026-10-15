package com.example.edtai.ui.bridge;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Shell;

public final class ClipboardText {

    private ClipboardText() {
    }

    public static void copy(Shell shell, String value) {
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
}
