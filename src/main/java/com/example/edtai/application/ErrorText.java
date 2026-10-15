package com.example.edtai.application;

import java.util.concurrent.CompletionException;

final class ErrorText {

    private ErrorText() {
    }

    static String describe(Throwable error) {
        // Извлекает понятный текст ошибки
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }
}
