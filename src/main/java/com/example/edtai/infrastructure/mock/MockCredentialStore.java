package com.example.edtai.infrastructure.mock;

import com.example.edtai.application.port.CredentialStore;

public final class MockCredentialStore implements CredentialStore {

    @Override
    public void saveApiKey(String agentSessionFile, String provider, String key) {
        // Игнорирует ключ в демо-режиме
    }
}
