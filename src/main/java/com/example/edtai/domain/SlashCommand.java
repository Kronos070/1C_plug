package com.example.edtai.domain;

public record SlashCommand(String name, String description, String hint, Kind kind) {

    public enum Kind {
        CLIENT,
        PLUGIN,
        COMMAND,
        PROMPT
    }
}
