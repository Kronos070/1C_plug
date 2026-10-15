package com.example.edtai.domain;

import java.util.List;
import java.util.stream.Collectors;

public record Message(String id, Role role, List<MessagePart> parts, long timestamp) {

    public Message {
        parts = List.copyOf(parts);
    }

    public String plainText() {
        // Склеивает текстовые части сообщения
        return parts.stream()
                .filter(MessagePart.Text.class::isInstance)
                .map(part -> ((MessagePart.Text) part).text())
                .collect(Collectors.joining("\n\n"));
    }
}
