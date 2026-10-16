package com.example.edtai.ui.feedback;

import com.example.edtai.application.port.AgentGateway;
import com.example.edtai.bootstrap.FeedbackAssembler;
import com.example.edtai.domain.FeedbackKind;
import java.util.List;
import java.util.Optional;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

public final class FeedbackHandler extends AbstractHandler {

    private static final String KIND = "com.example.edtai.feedbackKind";
    private static final String ORIGIN = "com.example.edtai.feedbackOrigin";

    @Override
    public Object execute(ExecutionEvent event) {
        // Ставит оценку или открывает окно отзыва
        boolean fromRuler = "ruler".equals(event.getParameter(ORIGIN));
        IEditorPart part = HandlerUtil.getActiveEditor(event);
        Optional<ITextEditor> editor = CodeTargets.textEditor(part);
        Optional<CodeTarget> target = editor.flatMap(value -> CodeTargets.resolve(value, fromRuler));
        if (target.isEmpty()) {
            Shell shell = HandlerUtil.getActiveShell(event);
            MessageDialog.openInformation(shell, "Отзыв о коде Перуна", editor.isEmpty()
                    ? "Откройте модуль в редакторе, чтобы оставить отзыв."
                    : "Не удалось определить код для отзыва. Выделите строки и повторите.");
            return null;
        }
        AgentGateway gateway = FeedbackAssembler.gateway();
        Optional<FeedbackKind> quick = FeedbackKind.fromId(event.getParameter(KIND));
        if (quick.isPresent()) {
            FeedbackSubmitter.submit(target.get(), new FeedbackInput(quick.get(), List.of(), ""), gateway);
        } else {
            FeedbackPopup.open(target.get(), FeedbackKind.DISLIKE,
                    input -> FeedbackSubmitter.submit(target.get(), input, gateway));
        }
        return null;
    }
}
