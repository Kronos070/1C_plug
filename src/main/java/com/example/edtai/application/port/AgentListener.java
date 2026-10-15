package com.example.edtai.application.port;

import com.example.edtai.domain.ChatEvent;

@FunctionalInterface
public interface AgentListener {

    void onEvent(ChatEvent event);
}
