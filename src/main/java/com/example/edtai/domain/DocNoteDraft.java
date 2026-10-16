package com.example.edtai.domain;

import java.util.List;

public record DocNoteDraft(String summary, String details, List<DocCase> cases) {

    public DocNoteDraft {
        summary = summary == null ? "" : summary.strip();
        details = details == null ? "" : details.strip();
        cases = cases == null ? List.of() : cases.stream().filter(item -> !item.isBlank()).toList();
    }
}
