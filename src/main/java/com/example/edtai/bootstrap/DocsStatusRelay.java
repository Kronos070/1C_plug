package com.example.edtai.bootstrap;

import com.example.edtai.application.DocsSignals;
import com.example.edtai.application.port.ChatOutput;
import com.example.edtai.domain.ChatEvent;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class DocsStatusRelay implements ChatOutput {

    private static final String DOCS_STATUS = "1c-docs";
    private static final String DOCS_WRITE_TOOL = "1c_docs_write";

    private final ChatOutput target;
    private final Set<String> docWrites = ConcurrentHashMap.newKeySet();

    DocsStatusRelay(ChatOutput target) {
        this.target = target;
    }

    @Override
    public void render(ChatEvent event) {
        // Пересылает статус индексации и сохранения описаний агентом
        if (event instanceof ChatEvent.StatusChanged status && DOCS_STATUS.equals(status.key())) {
            DocsSignals.publish(status.text());
        } else if (event instanceof ChatEvent.Connected) {
            DocsSignals.publish(DocsSignals.RECONNECTED);
        } else if (event instanceof ChatEvent.ToolStarted tool && DOCS_WRITE_TOOL.equals(tool.name())) {
            docWrites.add(tool.id());
        } else if (event instanceof ChatEvent.ToolFinished tool && docWrites.remove(tool.id()) && !tool.failed()) {
            DocsSignals.publish(DocsSignals.UPDATED);
        }
        target.render(event);
    }
}
