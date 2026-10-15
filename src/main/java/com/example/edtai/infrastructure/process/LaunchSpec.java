package com.example.edtai.infrastructure.process;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record LaunchSpec(
        String displayName,
        List<String> command,
        Path workDirectory,
        Map<String, String> environment) {

    public LaunchSpec {
        command = List.copyOf(command);
        environment = Map.copyOf(environment);
    }
}
