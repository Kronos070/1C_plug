package com.example.edtai.ui.feedback;

import com.example.edtai.bootstrap.FeedbackAssembler;
import com.example.edtai.domain.AiProvenance;
import com.example.edtai.domain.Feedback;
import com.example.edtai.domain.FeedbackKind;
import com.example.edtai.infrastructure.marks.AiMarkers;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

final class FeedbackSubmitter {

    private FeedbackSubmitter() {
    }

    static void submit(CodeTarget target, FeedbackInput input) {
        // Сохраняет отзыв, правило и оценку метки
        IMarker marker = target.marker().orElse(null);
        String original = marker == null ? target.code() : marker.getAttribute(AiMarkers.SNAPSHOT, target.code());
        AiProvenance provenance = marker == null ? new AiProvenance("", "", "") : AiMarkers.provenance(marker);
        Feedback feedback = new Feedback(input.kind(), "code", target.path(), target.startLine(), target.endLine(),
                input.categories(), input.comment(), original, "",
                provenance, marker == null ? "" : marker.getAttribute(AiMarkers.FRAGMENT, ""));
        rate(marker, input.kind());
        CompletableFuture.runAsync(() -> {
            try {
                FeedbackAssembler.service().submit(feedback);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }).whenComplete((ignored, error) -> status(target, error == null
                ? "Перун: отзыв сохранен"
                : "Перун: не удалось сохранить отзыв — " + error.getMessage()));
    }

    private static void rate(IMarker marker, FeedbackKind kind) {
        // Отмечает оценку на метке кода ИИ
        if (marker == null || !marker.exists()) {
            return;
        }
        try {
            marker.setAttribute(AiMarkers.RATING, kind.id());
        } catch (CoreException ignored) {
            return;
        }
    }

    private static void status(CodeTarget target, String message) {
        // Показывает результат в строке состояния
        target.editor().getSite().getShell().getDisplay().asyncExec(() -> target.editor().getEditorSite()
                .getActionBars().getStatusLineManager().setMessage(message));
    }
}
