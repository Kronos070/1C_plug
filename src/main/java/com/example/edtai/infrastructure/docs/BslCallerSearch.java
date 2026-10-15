package com.example.edtai.infrastructure.docs;

import com.example.edtai.application.port.CallerSearch;
import com.example.edtai.domain.BslSymbolLocator;
import com.example.edtai.domain.DocCaller;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class BslCallerSearch implements CallerSearch {

    private static final int LIMIT = 40;
    private static final String OWN = "(?<![\\p{L}\\p{N}_.])";
    private static final String OTHER = "(?<![\\p{L}\\p{N}_])";
    private static final Pattern COMMON_MODULE = Pattern.compile("CommonModules/([^/]+)/Module\\.bsl$");

    private final Supplier<Path> root;

    public BslCallerSearch(Supplier<Path> root) {
        this.root = root;
    }

    @Override
    public List<DocCaller> callers(String path, String name, boolean exported) {
        // Ищет методы, которые вызывают данный
        Path base = root.get();
        Pattern local = pattern(OWN, name);
        Matcher common = COMMON_MODULE.matcher(path.replace('\\', '/'));
        Pattern remote = pattern(common.find() ? OTHER + Pattern.quote(common.group(1)) + "\\s*\\.\\s*" : OTHER, name);
        Map<String, DocCaller> found = new LinkedHashMap<>();
        for (Path file : files(base, path, exported)) {
            String relative = base.relativize(file).toString().replace('\\', '/');
            scan(file, relative, name, relative.equals(path) ? local : remote, found);
            if (found.size() >= LIMIT) {
                break;
            }
        }
        return new ArrayList<>(found.values());
    }

    private static Pattern pattern(String prefix, String name) {
        // Строит выражение вызова метода
        return Pattern.compile(prefix + Pattern.quote(name) + "\\s*\\(",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    private static List<Path> files(Path base, String path, boolean exported) {
        // Выбирает модули, где искать вызовы
        if (!exported) {
            Path own = base.resolve(path);
            return Files.isRegularFile(own) ? List.of(own) : List.of();
        }
        try (Stream<Path> walk = Files.walk(base)) {
            String git = File.separator + ".git" + File.separator;
            return walk.filter(file -> file.toString().endsWith(".bsl"))
                    .filter(file -> !file.toString().contains(git)).sorted().toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static void scan(Path file, String relative, String name, Pattern call, Map<String, DocCaller> found) {
        // Находит вызовы в одном модуле
        String[] lines;
        try {
            lines = Files.readString(file, StandardCharsets.UTF_8).replace("﻿", "").split("\\R", -1);
        } catch (IOException e) {
            return;
        }
        for (int i = 0; i < lines.length && found.size() < LIMIT; i++) {
            String line = lines[i];
            if (line.strip().startsWith("//") || BslSymbolLocator.declares(line, name) || !call.matcher(line).find()) {
                continue;
            }
            String method = BslSymbolLocator.symbolAt(lines, i).orElse("");
            if (method.isEmpty() || method.equalsIgnoreCase(name)) {
                continue;
            }
            String id = relative + "#" + method;
            found.putIfAbsent(id, new DocCaller(id, method, relative, i + 1));
        }
    }
}
