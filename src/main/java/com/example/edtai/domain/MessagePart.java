package com.example.edtai.domain;

public sealed interface MessagePart {

    record Text(String text) implements MessagePart {
    }

    record Thinking(String text) implements MessagePart {
    }

    record Tool(String id, String name, String arguments, String output, boolean failed) implements MessagePart {
    }
}
