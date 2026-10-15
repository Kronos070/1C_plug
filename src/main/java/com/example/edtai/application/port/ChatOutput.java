package com.example.edtai.application.port;

import com.example.edtai.domain.ChatEvent;

@FunctionalInterface
public interface ChatOutput {

    void render(ChatEvent event);
}
