package com.example.edtai.application.port;

import com.example.edtai.domain.AiProvenance;
import java.util.List;

@FunctionalInterface
public interface AiCodeMarks {

    void markWritten(String path, List<String> snippets, AiProvenance provenance);
}
