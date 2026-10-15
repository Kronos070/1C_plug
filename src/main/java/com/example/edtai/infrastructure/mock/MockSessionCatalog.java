package com.example.edtai.infrastructure.mock;

import com.example.edtai.application.port.SessionCatalog;
import com.example.edtai.domain.SessionSummary;
import java.util.List;

public final class MockSessionCatalog implements SessionCatalog {

    private final MockStore store;

    MockSessionCatalog(MockStore store) {
        this.store = store;
    }

    @Override
    public List<SessionSummary> list(String currentSessionFile) {
        // Возвращает демо-сессии из памяти
        return store.summaries();
    }
}
