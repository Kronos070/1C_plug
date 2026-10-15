package com.example.edtai.application.port;

import com.example.edtai.domain.AiProvenance;
import com.example.edtai.domain.LineRange;
import java.util.List;

public interface AiCodeMarks {

    void markWritten(String path, List<String> snippets, List<LineRange> ranges, AiProvenance provenance);

    default void markWritten(String path, List<String> snippets, AiProvenance provenance) {
        markWritten(path, snippets, List.of(), provenance);
    }
}
