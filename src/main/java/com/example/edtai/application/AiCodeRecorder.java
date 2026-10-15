package com.example.edtai.application;

import com.example.edtai.application.port.AiCodeMarks;
import com.example.edtai.application.port.CodeReview;
import com.example.edtai.domain.AgentState;
import com.example.edtai.domain.AiProvenance;
import com.example.edtai.domain.ChatEvent;
import java.util.function.Supplier;

final class AiCodeRecorder {

    private final AiCodeMarks marks;
    private final CodeReview review;
    private final Supplier<AgentState> state;
    private volatile String lastPrompt = "";

    AiCodeRecorder(AiCodeMarks marks, CodeReview review, Supplier<AgentState> state) {
        this.marks = marks;
        this.review = review;
        this.state = state;
    }

    void remember(String prompt) {
        // Запоминает последний промпт пользователя
        lastPrompt = prompt;
    }

    boolean accept(ChatEvent event) {
        // Ставит метки на код, записанный агентом
        if (event instanceof ChatEvent.CodeWriting writing) {
            review.writing(writing.path(), writing.tool());
            return true;
        }
        if (!(event instanceof ChatEvent.CodeWritten written)) {
            return false;
        }
        review.written(written.path(), written.tool(), written.patch());
        AgentState current = state.get();
        String model = current == null || !current.model().isKnown() ? "" : current.model().key();
        marks.markWritten(written.path(), written.snippets(), written.ranges(),
                new AiProvenance(model, lastPrompt, "agent"));
        return true;
    }
}
