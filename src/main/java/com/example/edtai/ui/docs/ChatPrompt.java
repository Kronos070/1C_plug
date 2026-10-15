package com.example.edtai.ui.docs;

import com.example.edtai.ui.ChatView;
import java.util.Optional;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

final class ChatPrompt {

    private static final String CHAT_VIEW = "com.example.edtai.view.chat";

    private ChatPrompt() {
    }

    static Optional<String> send(String text) {
        // Отправляет запрос в чат ПерУН
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage page = window == null ? null : window.getActivePage();
        if (page == null || text.isBlank()) {
            return Optional.of("Не удалось открыть чат ПерУН");
        }
        try {
            IViewPart view = page.showView(CHAT_VIEW);
            if (view instanceof ChatView chat) {
                chat.prompt(text);
                return Optional.empty();
            }
            return Optional.of("Чат ПерУН недоступен");
        } catch (PartInitException e) {
            return Optional.of("Не удалось открыть чат ПерУН: " + e.getMessage());
        }
    }
}
