package com.example.edtai.infrastructure.process;

import com.example.edtai.application.port.AgentUnavailableException;

@FunctionalInterface
public interface LaunchSpecSource {

    LaunchSpec resolve() throws AgentUnavailableException;
}
