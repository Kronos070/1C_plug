package com.example.edtai.infrastructure.feedback;

import com.example.edtai.application.port.RuleStore;
import com.example.edtai.domain.ProjectRule;
import com.example.edtai.domain.RuleScope;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class MarkdownRuleStore implements RuleStore {

    private final Supplier<Path> root;

    public MarkdownRuleStore(Supplier<Path> root) {
        this.root = root;
    }

    @Override
    public List<ProjectRule> rules() {
        // Читает правила из файлов Markdown
        Path folder = root.get().resolve("rules");
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(file -> file.getFileName().toString().endsWith(".md"))
                    .sorted()
                    .map(MarkdownRuleStore::read)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    @Override
    public void save(ProjectRule rule) throws IOException {
        // Записывает правило отдельным файлом
        Path folder = root.get().resolve("rules");
        Files.createDirectories(folder);
        String text = "---\nscope: " + rule.scope().id() + "\ntarget: " + MarkdownText.quote(rule.target())
                + "\nauthor: " + MarkdownText.quote(System.getProperty("user.name", "")) + "\ncreated: "
                + MarkdownText.now() + "\n---\n\n" + rule.text() + "\n";
        Files.writeString(folder.resolve(MarkdownText.fileName("rule")), text, StandardCharsets.UTF_8);
    }

    private static ProjectRule read(Path file) {
        // Разбирает файл правила с шапкой
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8).replace("\r\n", "\n");
            Map<String, String> header = new HashMap<>();
            String body = text;
            if (text.startsWith("---\n")) {
                int end = text.indexOf("\n---", 4);
                if (end > 0) {
                    for (String line : text.substring(4, end).split("\n")) {
                        int colon = line.indexOf(':');
                        if (colon > 0) {
                            String value = MarkdownText.unquote(line.substring(colon + 1));
                            header.put(line.substring(0, colon).strip(), value);
                        }
                    }
                    body = text.substring(Math.min(text.length(), end + 4));
                }
            }
            ProjectRule rule = new ProjectRule(RuleScope.fromId(header.getOrDefault("scope", "config")),
                    header.getOrDefault("target", ""), body);
            return rule.text().isEmpty() ? null : rule;
        } catch (IOException e) {
            return null;
        }
    }
}
