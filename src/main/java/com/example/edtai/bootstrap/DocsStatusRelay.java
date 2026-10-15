package com.example.edtai.bootstrap;

import com.example.edtai.application.DocsSignals;
import com.example.edtai.application.port.ChatOutput;
import com.example.edtai.domain.ChatEvent;

final class DocsStatusRelay implements ChatOutput {

    private static final String DOCS_STATUS = "1c-docs";

    private final ChatOutput target;

    DocsStatusRelay(ChatOutput target) {
        this.target = target;
    }

    @Override
    public void render(ChatEvent event) {
        // Пересылает статус индексации документации
        if (event instanceof ChatEvent.StatusChanged status && DOCS_STATUS.equals(status.key())) {
            DocsSignals.publish(status.text());
        } else if (event instanceof ChatEvent.Connected) {
            DocsSignals.publish(DocsSignals.RECONNECTED);
        }
        target.render(event);
    }
}
