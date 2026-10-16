package com.example.edtai.bootstrap;

import com.example.edtai.application.DocsService;
import com.example.edtai.application.port.AgentGateway;
import com.example.edtai.application.port.DocsGateway;
import com.example.edtai.infrastructure.docs.BslCallerSearch;
import java.nio.file.Path;

public final class DocsAssembler {

    private static final DocsService SERVICE = new DocsService(DocsAssembler::gateway,
            new BslCallerSearch(DocsAssembler::workDirectory));

    private DocsAssembler() {
    }

    public static DocsService service() {
        // Возвращает сервис документации
        return SERVICE;
    }

    private static DocsGateway gateway() {
        // Возвращает шлюз документации агента
        AgentGateway agent = FeedbackAssembler.gateway();
        return agent == null ? null : agent.docs();
    }

    public static Path workDirectory() {
        // Возвращает рабочий каталог агента
        DocsGateway docs = gateway();
        return docs == null ? Path.of("") : docs.workDirectory();
    }
}
