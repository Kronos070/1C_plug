package com.example.edtai.domain;

import java.util.List;

public record UiRequest(String id, Kind kind, String title, String message, List<String> options, String prefill) {

    public enum Kind {
        CONFIRM,
        SELECT,
        INPUT,
        EDITOR
    }

    public UiRequest {
        options = List.copyOf(options);
    }
}
