package com.example.edtai.ui.marks;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

public final class ToggleMarksHandler extends AbstractHandler {

    public static final String COMMAND = "com.example.edtai.toggleMarks";

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        // Переключает подсветку кода ИИ
        boolean wasVisible = HandlerUtil.toggleCommandState(event.getCommand());
        MarksVisibility.set(!wasVisible);
        return null;
    }
}
