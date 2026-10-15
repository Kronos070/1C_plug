package com.example.edtai.infrastructure.settings;

public record PluginSettings(String command, String arguments, String workDirectory, boolean demoMode,
        String theme) {
}
