package com.example.edtai.application.port;

import java.io.IOException;

@FunctionalInterface
public interface CredentialStore {

    void saveApiKey(String agentSessionFile, String provider, String key) throws IOException;
}
