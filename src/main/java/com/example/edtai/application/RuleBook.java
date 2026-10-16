package com.example.edtai.application;

import com.example.edtai.domain.CodeContext;
import com.example.edtai.domain.ProjectRule;
import com.example.edtai.domain.RuleScope;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class RuleBook {

    private static final Pattern MENTION = Pattern.compile("(?:^|\\s)@(\\S+)");

    private RuleBook() {
    }

    static String block(List<ProjectRule> rules, String text, Optional<CodeContext> context) {
        // Собирает подходящие правила в блок промпта
        List<String> paths = new ArrayList<>();
        context.ifPresent(value -> paths.add(value.path()));
        Matcher matcher = MENTION.matcher(text);
        while (matcher.find()) {
            paths.add(matcher.group(1));
        }
        String lines = rules.stream()
                .filter(rule -> rule.scope() == RuleScope.CONFIG
                        || paths.stream().anyMatch(rule::appliesTo))
                .map(rule -> "- " + rule.text().replace("\n", " "))
                .distinct()
                .collect(Collectors.joining("\n"));
        return lines.isEmpty() ? "" : "<project-rules>\nПравила проекта, обязательные к соблюдению:\n" + lines
                + "\n</project-rules>";
    }
}
