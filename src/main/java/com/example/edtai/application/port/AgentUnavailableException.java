package com.example.edtai.application.port;

public final class AgentUnavailableException extends Exception {

    private static final long serialVersionUID = 1L;

    public AgentUnavailableException(String message) {
        super(message);
    }

    public AgentUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
