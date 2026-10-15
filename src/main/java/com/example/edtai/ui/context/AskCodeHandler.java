package com.example.edtai.ui.context;

import com.example.edtai.domain.CodeAction;
import com.example.edtai.domain.CodeContext;
import com.example.edtai.ui.ChatView;
import com.example.edtai.ui.layout.ChatSlot;
import java.util.Optional;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

public final class AskCodeHandler extends AbstractHandler {

    private static final String PARAMETER = "com.example.edtai.codeAction";

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        // Отправляет код из редактора в чат
        IEditorPart editor = HandlerUtil.getActiveEditor(event);
        Optional<CodeAction> action = CodeAction.fromId(event.getParameter(PARAMETER));
        Optional<CodeContext> context = editor == null ? Optional.empty() : EditorContextReader.read(editor);
        if (action.isEmpty() || context.isEmpty()) {
            return null;
        }
        try {
            IViewPart view = editor.getSite().getPage().showView(ChatSlot.VIEW_ID);
            if (view instanceof ChatView chat) {
                chat.ask(action.get(), context.get());
            }
        } catch (PartInitException e) {
            throw new ExecutionException(e.getMessage(), e);
        }
        return null;
    }
}
