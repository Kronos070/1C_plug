package com.example.edtai.bootstrap;

import com.example.edtai.application.DocsService;
import com.example.edtai.application.port.AgentGateway;

public final class DocsAssembler {

    private static final DocsService SERVICE = new DocsService(() -> {
        AgentGateway gateway = FeedbackAssembler.gateway();
        return gateway == null ? null : gateway.docs();
    });

    private DocsAssembler() {
    }

    public static DocsService service() {
        // Возвращает сервис документации
        return SERVICE;
    }
}
