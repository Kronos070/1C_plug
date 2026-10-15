package com.example.edtai.application;

import com.example.edtai.domain.CodeContext;
import com.example.edtai.domain.ProjectRule;
import java.util.List;
import java.util.Optional;

final class RuleBook {

    private RuleBook() {
    }

    static String block(List<ProjectRule> rules, String text, Optional<CodeContext> context) {
        // Правила проекта теперь загружаются агентом напрямую из .perun/rules/
        return "";
    }
}
