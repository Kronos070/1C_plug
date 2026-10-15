package com.example.edtai.application.port;

import com.example.edtai.domain.DiffHunk;
import java.util.List;

public interface ReviewMarks {

    void show(String path, List<DiffHunk> hunks);

    void clear(String path, boolean dropAiCode);
}
