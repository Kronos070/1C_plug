package com.example.edtai.infrastructure.process;

import com.example.edtai.application.port.AgentUnavailableException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class LaunchSpecFactory {

    private static final List<String> KNOWN_AGENTS = List.of("perun", "pi");

    private final ExecutableLocator locator;

    public LaunchSpecFactory(ExecutableLocator locator) {
        this.locator = locator;
    }

    public LaunchSpec create(String command, String arguments, Path workDirectory) throws AgentUnavailableException {
        // Собирает команду запуска агента
        List<String> base = command.isBlank() ? detect() : explicit(command);
        List<String> full = new ArrayList<>(base);
        full.add("--mode");
        full.add("rpc");
        full.addAll(CommandLine.split(arguments));
        String name = Path.of(base.get(0)).getFileName().toString();
        Map<String, String> environment = Map.of("PATH", locator.searchPath());
        return new LaunchSpec(name, full, workDirectory, environment);
    }

    private List<String> detect() throws AgentUnavailableException {
        // Ищет установленного агента автоматически
        for (String name : KNOWN_AGENTS) {
            Optional<Path> found = locator.find(name);
            if (found.isPresent()) {
                return List.of(found.get().toString());
            }
        }
        throw new AgentUnavailableException("Агент не найден: perun или pi отсутствуют в PATH. "
                + "Укажите команду запуска в «Окно → Параметры → ПерУН» или включите демо-режим.");
    }

    private List<String> explicit(String command) throws AgentUnavailableException {
        // Разбирает команду из настроек
        List<String> parts = new ArrayList<>(CommandLine.split(command));
        if (parts.isEmpty()) {
            throw new AgentUnavailableException("Команда запуска агента пуста");
        }
        String program = parts.get(0);
        if (!program.contains("/") && !program.contains("\\")) {
            locator.find(program).ifPresent(path -> parts.set(0, path.toString()));
        }
        if (program.endsWith(".mjs") || program.endsWith(".js")) {
            String node = locator.find("node").map(Path::toString).orElse("node");
            parts.add(0, node);
        }
        return parts;
    }
}
