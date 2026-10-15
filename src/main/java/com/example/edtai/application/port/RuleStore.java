package com.example.edtai.application.port;

import com.example.edtai.domain.ProjectRule;
import java.io.IOException;
import java.util.List;

public interface RuleStore {

    List<ProjectRule> rules();

    void save(ProjectRule rule) throws IOException;
}
